package com.simbiocreacion.resource.controller;

import com.simbiocreacion.resource.model.User;
import com.simbiocreacion.resource.service.ISymbiocreationService;
import com.simbiocreacion.resource.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bson.Document;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Log4j2
public class AnalyticsController {

    private final ISymbiocreationService symbiocreationService;

    private final IUserService userService;

    @GetMapping("/analytics/get-trending-topics")
    public Mono<String> sendDataForTopicAnalysis() throws IOException {
        final Path tempPath = Files.createTempFile("ideas-data", ".tmp");
        // Create file to upload
        Mono<Void> ideasWriter = this.symbiocreationService.getIdeasAll()
                .doOnNext(idea -> {
                    String ideaLine = String.format("%s  %s", idea.getTitle(), idea.getDescription());
                    try {
                        Files.write(tempPath, ideaLine.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .doOnComplete(() -> log.info("Finished creating temp file"))
                .then();

        //MultiValueMap<String, String> bodyValues = new LinkedMultiValueMap<>();
        //bodyValues.add("Content-Type", "text/plain");
        //bodyValues.add("another-key", "another-value");

        WebClient client = WebClient.create();
        Mono<String> response = client.put()
                .uri("https://symbio-comprehend-bucket.s3.us-east-1.amazonaws.com")
                .header("Content-Type", "text/plain")
                .header("Authorization", )
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.APPLICATION_JSON)
                //.body(BodyInserters.fromFormData(bodyValues))
                .retrieve()
                .bodyToMono(String.class);

        return ideasWriter
                .then(response);
    }

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
                        document.append("_id", "2022-10-10");
                    }
                    return document;
                });
    }

    @GetMapping("/analytics/user-counts-daily-chart")
    public Flux<Document> getUserCountsDaily() {

        return userService.groupAndCountByDate()
                .map(document -> {
                    if (document.getString("_id") == null) {
                        document.append("_id", "2022-10-10");
                    }
                    return document;
                });
    }

    @GetMapping("/analytics/trending-topics")
    public Flux<Document> getTrendingTopics() {

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
}
