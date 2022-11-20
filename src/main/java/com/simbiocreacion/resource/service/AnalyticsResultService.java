package com.simbiocreacion.resource.service;

import com.simbiocreacion.resource.model.AnalyticsResult;
import com.simbiocreacion.resource.repository.AnalyticsResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AnalyticsResultService implements IAnalyticsResultService {

    private final AnalyticsResultRepository analyticsResultRepository;

    @Override
    public Mono<AnalyticsResult> create(AnalyticsResult e) {
        return this.analyticsResultRepository.save(e);
    }

    @Override
    public Mono<AnalyticsResult> findById(String id) {
        return this.analyticsResultRepository.findById(id);
    }

    @Override
    public Flux<AnalyticsResult> findAll() {
        return this.analyticsResultRepository.findAll();
    }

    @Override
    public Mono<AnalyticsResult> update(AnalyticsResult e) {
        return this.analyticsResultRepository.save(e);
    }

    @Override
    public Mono<Void> delete(String id) {
        return this.analyticsResultRepository.deleteById(id);
    }

    @Override
    public Mono<Void> deleteAll() {
        return this.analyticsResultRepository.deleteAll();
    }

    @Override
    public Mono<Long> count() {
        return this.analyticsResultRepository.count();
    }

    @Override
    public Mono<AnalyticsResult> findLastWithNonEmptyResults() {
        return this.analyticsResultRepository.findFirstByResultsFileContentIsNotNullOrderByCreationDateTimeDesc();
    }

    @Override
    public Mono<AnalyticsResult> findLastWithEmptyResults() {
        return this.analyticsResultRepository.findFirstByResultsFileContentIsNullOrderByCreationDateTimeDesc();
    }
}
