package com.simbiocreacion.resource.repository;

import com.simbiocreacion.resource.model.AnalyticsResult;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface AnalyticsResultRepository extends ReactiveMongoRepository<AnalyticsResult, String> {

    // fetch most recent record with non-null results
    Mono<AnalyticsResult> findFirstByResultsFileContentIsNotNullOrderByCreationDateTimeDesc();

    // fetch most recent record with null results
    Mono<AnalyticsResult> findFirstByResultsFileContentIsNullOrderByCreationDateTimeDesc();
}
