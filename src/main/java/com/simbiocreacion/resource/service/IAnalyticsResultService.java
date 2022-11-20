package com.simbiocreacion.resource.service;

import com.simbiocreacion.resource.model.AnalyticsResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IAnalyticsResultService {

    Mono<AnalyticsResult> create(AnalyticsResult e);

    Mono<AnalyticsResult> findById(String id);

    Flux<AnalyticsResult> findAll();

    Mono<AnalyticsResult> update(AnalyticsResult e);

    Mono<Void> delete(String id);

    Mono<Void> deleteAll();

    Mono<Long> count();

    Mono<AnalyticsResult> findLastWithNonEmptyResults();

    Mono<AnalyticsResult> findLastWithEmptyResults();
}
