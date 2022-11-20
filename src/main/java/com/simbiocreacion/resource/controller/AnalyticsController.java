package com.simbiocreacion.resource.controller;

import com.simbiocreacion.resource.model.AnalyticsResult;
import com.simbiocreacion.resource.model.User;
import com.simbiocreacion.resource.service.IAnalyticsResultService;
import com.simbiocreacion.resource.service.ISymbiocreationService;
import com.simbiocreacion.resource.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.bson.Document;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.comprehend.ComprehendClient;
import software.amazon.awssdk.services.comprehend.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Log4j2
public class AnalyticsController {

    private static final String awsAccountId = "206385053618";

    private final ISymbiocreationService symbiocreationService;

    private final IUserService userService;

    private final IAnalyticsResultService analyticsResultService;

    private final AwsBasicCredentials awsBasicCredentials;

    @GetMapping("/analytics/counts-summary")
    public Mono<Map<String, Long>> getCountsSummary() {
        final Map<String, Long> counts = new HashMap<>();

        return Mono.zip(
                symbiocreationService.count(),
                userService.count(),
                symbiocreationService.countIdeasAll()
        ).map(tuple3 -> {
            counts.put("symbiocreations", tuple3.getT1());
            counts.put("users", tuple3.getT2());
            counts.put("ideas", tuple3.getT3());

            return counts;
        }).defaultIfEmpty(new HashMap<>());
    }

    @GetMapping("/analytics/symbio-counts-daily-chart")
    public Flux<Document> getSymbioCountsDaily() {

        return symbiocreationService.groupAndCountByDate()
                .map(document -> {
                    if (document.getString("_id") == null) {
                        document.put("_id", "2022-10-10");
                    }
                    return document;
                });
    }

    @GetMapping("/analytics/user-counts-daily-chart")
    public Flux<Document> getUserCountsDaily() {

        return userService.groupAndCountByDate()
                .map(document -> {
                    if (document.getString("_id") == null) {
                        document.put("_id", "2022-10-10");
                    }
                    return document;
                });
    }

    @GetMapping("/analytics/trending-topics-ideas")
    public Flux<Document> getTrendingTopicsIdeas() {

        return Flux.empty();
    }

    @GetMapping("/analytics/top-symbiocreations")
    public Flux<Document> getTopSymbiocreations() {

        return symbiocreationService.getTopSymbiocreations();
    }

    @GetMapping("/analytics/top-users")
    public Flux<User> getTopUsers() {

        return userService.getTopUsers();
    }

    @GetMapping("/analytics/get-last-ideas-topic-modeling-result-database")
    public Mono<AnalyticsResult> getLastIdeasTopicModelingResultFromDatabase() {

        return this.analyticsResultService.findLastWithNonEmptyResults();
    }

    private String extractFileContentFromResponseInputStream(ResponseInputStream responseInputStream) {
        String fileContent = "";

        try (final TarArchiveInputStream tarInputStream =
                     new TarArchiveInputStream(
                             new GzipCompressorInputStream(
                                     new ByteArrayInputStream(responseInputStream.readAllBytes())))) {
            TarArchiveEntry currentEntry = tarInputStream.getNextTarEntry();

            // Just pick specific file from .tar file
            while (!currentEntry.getName().equals("topic-terms.csv")) {
                currentEntry = tarInputStream.getNextTarEntry();
            }

            // Read directly from TarArchiveInputStream
            BufferedReader br = new BufferedReader(new InputStreamReader(tarInputStream));
            fileContent = br.lines()
                    .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return fileContent;
    }


    // ============ Scheduled tasks for topic modeling ============

    // Run task every 4 days
//	@Scheduled(fixedRate=345600000, initialDelay=172800000)
    @Scheduled(fixedRate=345600000, initialDelay=5000)
    public void scheduleIdeasTopicModelingTask() throws IOException {
        long now = System.currentTimeMillis() / 1000;
        log.info("Executing cron job. Current time: {} seconds", now);

        // Analytics pipeline: AWS S3 + Comprehend
        this.sendDataForTopicAnalysisS3()
                .doOnSuccess(v -> log.info("Done sending data to S3"))
                .then(this.triggerTopicModelingJobComprehend())
                .doOnSuccess(v -> log.info("Done creating topic modeling job in AWS Comprehend"))
                .log()
                .subscribe();

        this.updateDbWithRemoteResultsOfLastPendingAnalyticsS3()
                .doOnSuccess(analyticsResult -> {
                    log.info("Done updating database with remote analytics results");
                    log.info("Analytics results: {}", analyticsResult);
                })
                .delaySubscription(Duration.ofHours(1))
                .log()
                .subscribe();
    }

    private Mono<Void> sendDataForTopicAnalysisS3() throws IOException {
        // AWS SDK to use S3 and Comprehend
        S3Client s3Client = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
                .region(Region.US_EAST_1)
                .build();

        // Create temporal file to upload
        final Path tempPath = Files.createTempFile("data-ideas", ".tmp");
        final StringBuffer stringBuffer = new StringBuffer();

        return this.symbiocreationService.getIdeasAll()
                .doOnNext(idea -> {
                    // Format data for AWS Comprehend: one doc per line
                    final String ideaTitle = idea.getTitle() != null ?
                            idea.getTitle().replaceAll("\\R+", " ") : "";
                    final String ideaDescription = idea.getDescription() != null ?
                            idea.getDescription().replaceAll("\\R+", " ") : "";

                    final String ideaLine = String.format("%s - %s\n", ideaTitle, ideaDescription);
                    stringBuffer.append(ideaLine);
                })
                .doOnComplete(() -> {
                    try {
                        // one-time write
                        Files.write(tempPath, stringBuffer.toString().getBytes(StandardCharsets.UTF_8));
                        log.info("Temp file created successfully");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                            .bucket("symbio-comprehend-bucket")
                            .key("input/data-ideas.txt")
                            .build();
                    PutObjectResponse response = s3Client.putObject(putObjectRequest, tempPath);
                    log.info(response);
                })
                .then();
    }

    private Mono<Void> triggerTopicModelingJobComprehend() {
        final ComprehendClient comprehendClient = ComprehendClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
                .region(Region.US_EAST_1)
                .build();

        final String inputS3Uri = "s3://symbio-comprehend-bucket/input";
        final InputFormat inputDocFormat = InputFormat.ONE_DOC_PER_LINE;
        final String outputS3Uri = "s3://symbio-comprehend-bucket/output";
        final String dataAccessRoleArn = String.format(
                "arn:aws:iam::%s:role/service-role/AmazonComprehendServiceRole-uploader",
                awsAccountId);
        final int numberOfTopics = 15;

        final StartTopicsDetectionJobRequest startTopicsDetectionJobRequest = StartTopicsDetectionJobRequest.builder()
                .inputDataConfig(InputDataConfig.builder()
                        .s3Uri(inputS3Uri)
                        .inputFormat(inputDocFormat)
                        .build())
                .outputDataConfig(OutputDataConfig.builder()
                        .s3Uri(outputS3Uri)
                        .build())
                .dataAccessRoleArn(dataAccessRoleArn)
                .numberOfTopics(numberOfTopics)
                .build();

        final StartTopicsDetectionJobResponse startTopicsDetectionJobResponse =
                comprehendClient.startTopicsDetectionJob(startTopicsDetectionJobRequest);

        final String jobId = startTopicsDetectionJobResponse.jobId();
        log.info("JobId: {}", jobId);

        // Store generated JobId in database for later retrieval
        final AnalyticsResult analyticsResult = AnalyticsResult.builder()
                .id(jobId)
                .analysisName("topic-modeling")
                .serviceName("aws-comprehend")
                .creationDateTime(new Date())
                .resultsFileContent(null)
                .build();
        this.analyticsResultService.create(analyticsResult).subscribe();
        // ==================

        final DescribeTopicsDetectionJobRequest describeTopicsDetectionJobRequest =
                DescribeTopicsDetectionJobRequest.builder()
                        .jobId(jobId)
                        .build();
        final DescribeTopicsDetectionJobResponse describeTopicsDetectionJobResponse =
                comprehendClient.describeTopicsDetectionJob(describeTopicsDetectionJobRequest);
        log.info("describeTopicsDetectionJobResponse: {}", describeTopicsDetectionJobResponse);

        ListTopicsDetectionJobsResponse listTopicsDetectionJobsResponse =
                comprehendClient.listTopicsDetectionJobs(ListTopicsDetectionJobsRequest.builder().build());
        log.info("listTopicsDetectionJobsResponse: {}", listTopicsDetectionJobsResponse);

        return Mono.empty();
    }

    private Mono<AnalyticsResult> updateDbWithRemoteResultsOfLastPendingAnalyticsS3() {
        final S3Client s3Client = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
                .region(Region.US_EAST_1)
                .build();

        return this.analyticsResultService.findLastWithEmptyResults()
                .flatMap(analyticsResult -> {
                    GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                            .bucket("symbio-comprehend-bucket")
                            .key(String.format(
                                    "output/%s-TOPICS-%s/output/output.tar.gz",
                                    awsAccountId,
                                    analyticsResult.getId()))
                            .build();
                    ResponseInputStream<GetObjectResponse> responseInputStream = s3Client.getObject(getObjectRequest);
                    log.info(responseInputStream.response());

                    final String fileContent = this.extractFileContentFromResponseInputStream(responseInputStream);
//                    log.debug("File content to be stored: \n {}", fileContent);
                    // set results property of analyticsResult
                    analyticsResult.setResultsFileContent(fileContent);

                    return this.analyticsResultService.update(analyticsResult);
                });
    }
}
