package com.simbiocreacion.resource.controller;

import com.simbiocreacion.resource.dto.TrendAiResponse;
import com.simbiocreacion.resource.model.Node;
import com.simbiocreacion.resource.model.Symbiocreation;
import com.simbiocreacion.resource.model.User;
import com.simbiocreacion.resource.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private static final String awsAccountId = "206385053618";

    private final ISymbiocreationService symbiocreationService;
    private final IUserService userService;
    private final ILlmService llmService;
    private final IAnalyticsResultService analyticsResultService;

//    private final AwsBasicCredentials awsBasicCredentials;


    // =========== Analytics for Public ===========

    @GetMapping("/analytics/ranking-users-public")
    public Flux<User> getRankingUsersPublic() {
        return userService.getTopUsersRankingPublic();
    }

    // =========== Analytics for Admin Dashboard ===========

    @GetMapping("/analytics/counts-summary-admin")
    public Mono<Map<String, Long>> getCountsSummaryAdmin() {
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

    @GetMapping("/analytics/symbio-counts-daily-chart-admin")
    public Flux<Document> getSymbioCountsDaily() {

        return symbiocreationService.groupAndCountByDate()
                .map(document -> {
                    if (document.getString("_id") == null) {
                        document.put("_id", "2022-10-10");
                    }
                    return document;
                });
    }

    @GetMapping("/analytics/user-counts-daily-chart-admin")
    public Flux<Document> getUserCountsDaily() {

        return userService.groupAndCountByDate()
                .map(document -> {
                    if (document.getString("_id") == null) {
                        document.put("_id", "2022-10-10");
                    }
                    return document;
                });
    }

    @GetMapping("/analytics/trending-topics-ideas-admin")
    public Flux<Document> getTrendingTopicsIdeasAdmin() {

        return this.analyticsResultService.findLastWithNonEmptyResults()
                .flatMapIterable(analyticsResult -> {
                    List<Document> documents = new ArrayList<>();

                    analyticsResult.getResultsFileContent().lines()
                            .filter(line -> !line.startsWith("topic"))
                            .forEach(line -> {
                                final String[] values = line.split(",");

                                if (documents.size() == Integer.valueOf(values[0])) {
                                    Document newDocument = new Document();
                                    newDocument.put("topic", Integer.valueOf(values[0]));
                                    newDocument.put("terms", new ArrayList<>());
//                                    newDocument.put("termCounts", new ArrayList<>());

                                    documents.add(new Document(newDocument));
                                }

                                Document currentDocument = documents.get(Integer.valueOf(values[0]));
                                currentDocument.getList("terms", String.class).add(values[1]);
                            });

                    return documents;
                })
                .flatMap(this::completeTermCountsInDocument);
    }

    private Mono<Document> completeTermCountsInDocument(Document document) {
        final List<String> documentTerms = document.getList("terms", String.class);

        final Map<String, Integer> counts = new HashMap<>();
        counts.put(documentTerms.get(0), 0); // first 4 words of each topic
        counts.put(documentTerms.get(1), 0);
        counts.put(documentTerms.get(2), 0);
        counts.put(documentTerms.get(3), 0);

        return this.symbiocreationService.getIdeasAllVisibilityPublic()
                .doOnNext(idea -> {
                    final String ideaTitle = idea.getTitle() != null ?
                            idea.getTitle().replaceAll("\\R+", " ") : "";
                    final String ideaDescription = idea.getDescription() != null ?
                            idea.getDescription().replaceAll("\\R+", " ") : "";
                    final String ideaLine = String.format("%s - %s\n", ideaTitle, ideaDescription).toLowerCase();

                    for (String word : ideaLine.split("[^\\p{L}]+")) {
                        if (counts.containsKey(word)) {
                            counts.put(word, counts.get(word) + 1);
                        }
                    }
                })
                .doOnComplete(() -> document.put("termCounts", List.of(
                        counts.get(documentTerms.get(0)),
                        counts.get(documentTerms.get(1)),
                        counts.get(documentTerms.get(2)),
                        counts.get(documentTerms.get(3))
                    )))
                .then(Mono.just(document));
    }

    @GetMapping("/analytics/top-symbiocreations-admin")
    public Flux<Document> getTopSymbiocreationsAdmin() {

        return symbiocreationService.getTopSymbiocreations();
    }

    @GetMapping("/analytics/top-users-admin")
    public Flux<User> getTopUsersAdmin() {
        return userService.getTopUsersAdmin();
    }


    // =========== Analytics for User Dashboard ===========

    @GetMapping("/analytics/counts-summary-user/{userId}")
    public Mono<Map<String, Long>> getCountsSummaryUser(@PathVariable String userId) {
        final Map<String, Long> counts = new HashMap<>();

        return Mono.zip(
                userService.findById(userId), // User's score
                symbiocreationService.countByUser(userId), // Total symbiocreations
                symbiocreationService.countIdeasAllOfUser(userId), // Total ideas
                symbiocreationService.countGroupsAsAmbassadorOfUser(userId)  // Total groups as ambassador
        ).map(tuple4 -> {
            counts.put("score", (long) tuple4.getT1().getScore());
            counts.put("symbiocreations", tuple4.getT2());
            counts.put("ideas", tuple4.getT3());
            counts.put("groupsAsAmbassador", tuple4.getT4());

            return counts;
        }).defaultIfEmpty(new HashMap<>());
    }


    // =========== Analytics for Symbiocreation Dashboard ===========

    @GetMapping("/analytics/counts-summary-symbiocreation/{symbiocreationId}")
    public Mono<Map<String, Long>> getCountsSummarySymbiocreations(@PathVariable String symbiocreationId) {
        final Map<String, Long> counts = new HashMap<>();

        return Mono.zip(
                this.symbiocreationService.findById(symbiocreationId), // Symbiocreations's total users
                this.symbiocreationService.countIdeasAllOfSymbiocreation(symbiocreationId) // Total ideas
        ).map(tuple2 -> {
            counts.put("users", (long) tuple2.getT1().getParticipants().size());
            counts.put("ideas", tuple2.getT2());

            return counts;
        }).defaultIfEmpty(new HashMap<>());
    }

    @GetMapping("/analytics/users-ranking-symbiocreation/{symbiocreationId}")
    public Flux<Document> getUsersRankingSymbiocreation(@PathVariable String symbiocreationId) {

        return this.symbiocreationService.findById(symbiocreationId)
                .flatMapMany(this::computeUsersRankingOfSymbiocreation);
    }

    @GetMapping("/analytics/trends-symbiocreation/{symbiocreationId}")
    public Mono<List<TrendAiResponse>> getTrendsInSymbiocreation(@PathVariable String symbiocreationId) {

        return this.symbiocreationService.findById(symbiocreationId)
                .flatMap(this.llmService::getTrendsForSymbioFromLlm);
    }

    private Flux<Document> computeUsersRankingOfSymbiocreation(Symbiocreation symbiocreation) {
        final int coefParticipant = 1;
        final int coefAmbassador = 2;

        final Set<Node> roots = SymbiocreationService.findTreeRoots(symbiocreation);
        final Map<Node, Node> leafToRootMap = new HashMap<>();

        return Flux.fromStream(symbiocreation.getParticipants().stream())
                .flatMap(participant -> this.userService.findById(participant.getU_id()))
                .map(user -> {
                    // compute score of this user in current symbiocreation
                    int score = roots.stream()
                            .parallel()
                            .flatMap(root ->
                                    SymbiocreationService.findLeavesOf(root, new HashSet<>()).stream()
                                            .filter(leaf -> leaf.getU_id() != null && leaf.getU_id().equals(user.getId()))
                                            .map(leaf -> {
                                                leafToRootMap.put(leaf, root);
                                                return leaf;
                                            })
                            )
                            .mapToInt(leaf ->
                                    leaf.getRole() != null && leaf.getRole().equals("ambassador") ?
                                            coefAmbassador * SymbiocreationService.computeDepthOf(leaf, leafToRootMap.get(leaf)) :
                                            coefParticipant * SymbiocreationService.computeDepthOf(leaf, leafToRootMap.get(leaf))
                            )
                            .sum();

                    // Create Document with user and score
                    Document document = new Document();
                    document.put("user", user);
                    document.put("score", score);

                    return document;
                })
                .sort((document1, document2) -> document2.getInteger("score") - document1.getInteger("score"));
    }




    // ============ Scheduled tasks for topic modeling ============

    // Run task every 4 days, initial delay 1 hour
//    @Scheduled(fixedRate=345600000, initialDelay=3600000)
//    public void scheduleIdeasTopicModelingTask() throws IOException {
//        long now = System.currentTimeMillis() / 1000;
//        log.info("Executing cron job. Current time: {} seconds", now);
//
//        // Analytics pipeline: AWS S3 + Comprehend
//        this.sendDataForTopicAnalysisS3()
//                .doOnSuccess(v -> log.info("Done sending data to S3"))
//                .then(this.triggerTopicModelingJobComprehend())
//                .doOnSuccess(v -> log.info("Done creating topic modeling job in AWS Comprehend"))
//                .log()
//                .subscribe();
//
//        this.updateDbWithRemoteResultsOfLastPendingAnalyticsS3()
//                .doOnSuccess(analyticsResult -> {
//                    log.info("Done updating database with remote analytics results");
//                    log.info("Analytics results: {}", analyticsResult);
//                })
//                .delaySubscription(Duration.ofHours(1))
//                .log()
//                .subscribe();
//    }

//    private Mono<Void> sendDataForTopicAnalysisS3() throws IOException {
//        // AWS SDK to use S3 and Comprehend
//        S3Client s3Client = S3Client.builder()
//                .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
//                .region(Region.US_EAST_1)
//                .build();
//
//        // Create temporal file to upload
//        final Path tempPath = Files.createTempFile("data-ideas", ".tmp");
//        final StringBuffer stringBuffer = new StringBuffer();
//
//        return this.symbiocreationService.getIdeasAllVisibilityPublic()
//                .doOnNext(idea -> {
//                    // Format data for AWS Comprehend: one doc per line
//                    final String ideaTitle = idea.getTitle() != null ?
//                            idea.getTitle().replaceAll("\\R+", " ") : "";
//                    final String ideaDescription = idea.getDescription() != null ?
//                            idea.getDescription().replaceAll("\\R+", " ") : "";
//
//                    final String ideaLine = String.format("%s - %s\n", ideaTitle, ideaDescription);
//                    stringBuffer.append(ideaLine);
//                })
//                .doOnComplete(() -> {
//                    try {
//                        // one-time write
//                        Files.write(tempPath, stringBuffer.toString().getBytes(StandardCharsets.UTF_8));
//                        log.info("Temp file created successfully");
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//
//                    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
//                            .bucket("symbio-comprehend-bucket")
//                            .key("input/data-ideas.txt")
//                            .build();
//                    PutObjectResponse response = s3Client.putObject(putObjectRequest, tempPath);
////                    log.info(response);
//                })
//                .then();
//    }

//    private Mono<Void> triggerTopicModelingJobComprehend() {
//        final ComprehendClient comprehendClient = ComprehendClient.builder()
//                .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
//                .region(Region.US_EAST_1)
//                .build();
//
//        final String inputS3Uri = "s3://symbio-comprehend-bucket/input";
//        final InputFormat inputDocFormat = InputFormat.ONE_DOC_PER_LINE;
//        final String outputS3Uri = "s3://symbio-comprehend-bucket/output";
//        final String dataAccessRoleArn = String.format(
//                "arn:aws:iam::%s:role/service-role/AmazonComprehendServiceRole-uploader",
//                awsAccountId);
//        final int numberOfTopics = 15;
//
//        final StartTopicsDetectionJobRequest startTopicsDetectionJobRequest = StartTopicsDetectionJobRequest.builder()
//                .inputDataConfig(InputDataConfig.builder()
//                        .s3Uri(inputS3Uri)
//                        .inputFormat(inputDocFormat)
//                        .build())
//                .outputDataConfig(OutputDataConfig.builder()
//                        .s3Uri(outputS3Uri)
//                        .build())
//                .dataAccessRoleArn(dataAccessRoleArn)
//                .numberOfTopics(numberOfTopics)
//                .build();
//
//        final StartTopicsDetectionJobResponse startTopicsDetectionJobResponse =
//                comprehendClient.startTopicsDetectionJob(startTopicsDetectionJobRequest);
//
//        final String jobId = startTopicsDetectionJobResponse.jobId();
//        log.info("JobId: {}", jobId);
//
//        // Store generated JobId in database for later retrieval
//        final AnalyticsResult analyticsResult = AnalyticsResult.builder()
//                .id(jobId)
//                .analysisName("topic-modeling")
//                .serviceName("aws-comprehend")
//                .creationDateTime(new Date())
//                .resultsFileContent(null)
//                .build();
//        this.analyticsResultService.create(analyticsResult).subscribe();
//        // ==================
//
//        final DescribeTopicsDetectionJobRequest describeTopicsDetectionJobRequest =
//                DescribeTopicsDetectionJobRequest.builder()
//                        .jobId(jobId)
//                        .build();
//        final DescribeTopicsDetectionJobResponse describeTopicsDetectionJobResponse =
//                comprehendClient.describeTopicsDetectionJob(describeTopicsDetectionJobRequest);
//        log.info("describeTopicsDetectionJobResponse: {}", describeTopicsDetectionJobResponse);
//
//        ListTopicsDetectionJobsResponse listTopicsDetectionJobsResponse =
//                comprehendClient.listTopicsDetectionJobs(ListTopicsDetectionJobsRequest.builder().build());
//        log.info("listTopicsDetectionJobsResponse: {}", listTopicsDetectionJobsResponse);
//
//        return Mono.empty();
//    }

//    private Mono<AnalyticsResult> updateDbWithRemoteResultsOfLastPendingAnalyticsS3() {
//        final S3Client s3Client = S3Client.builder()
//                .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
//                .region(Region.US_EAST_1)
//                .build();
//
//        return this.analyticsResultService.findLastWithEmptyResults()
//                .flatMap(analyticsResult -> {
//                    GetObjectRequest getObjectRequest = GetObjectRequest.builder()
//                            .bucket("symbio-comprehend-bucket")
//                            .key(String.format(
//                                    "output/%s-TOPICS-%s/output/output.tar.gz",
//                                    awsAccountId,
//                                    analyticsResult.getId()))
//                            .build();
//                    ResponseInputStream<GetObjectResponse> responseInputStream = s3Client.getObject(getObjectRequest);
////                    log.info(responseInputStream.response());
//
//                    final String fileContent = this.extractFileContentFromResponseInputStream(responseInputStream);
////                    log.debug("File content to be stored: \n {}", fileContent);
//                    // set results property of analyticsResult
//                    analyticsResult.setResultsFileContent(fileContent);
//
//                    return this.analyticsResultService.update(analyticsResult);
//                });
//    }

//    private String extractFileContentFromResponseInputStream(ResponseInputStream responseInputStream) {
//        String fileContent = "";
//
//        try (final TarArchiveInputStream tarInputStream =
//                     new TarArchiveInputStream(
//                             new GzipCompressorInputStream(
//                                     new ByteArrayInputStream(responseInputStream.readAllBytes())))) {
//            TarArchiveEntry currentEntry = tarInputStream.getNextTarEntry();
//
//            // Just pick specific file from .tar file
//            while (!currentEntry.getName().equals("topic-terms.csv")) {
//                currentEntry = tarInputStream.getNextTarEntry();
//            }
//
//            // Read directly from TarArchiveInputStream
//            BufferedReader br = new BufferedReader(new InputStreamReader(tarInputStream));
//            fileContent = br.lines()
//                    .collect(Collectors.joining("\n"));
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//        return fileContent;
//    }
}
