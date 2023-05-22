package com.simbiocreacion.resource.service;

import com.simbiocreacion.resource.model.OneDot;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IOneDotService {

    Mono<OneDot> create(OneDot e);

    Mono<OneDot> findById(String id);

    Flux<OneDot> findAllByUser(String userId, Pageable pageable);

    Mono<OneDot> update(OneDot e);

    Mono<Long> countByUser(String userId);
}
