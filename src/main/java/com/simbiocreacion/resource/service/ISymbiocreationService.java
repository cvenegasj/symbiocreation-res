package com.simbiocreacion.resource.service;

import com.simbiocreacion.resource.model.Symbiocreation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ISymbiocreationService {

    Mono<Symbiocreation> create(Symbiocreation e);

    Mono<Symbiocreation> findById(String id);

    Flux<Symbiocreation> findAll();

    Flux<Symbiocreation> findByUserId(String userId);

    Flux<Symbiocreation> findByUserIdSingle(String userId);

    Mono<Symbiocreation> update(Symbiocreation e);

    Mono<Void> delete(String id);

    Mono<Void> deleteAll();
}
