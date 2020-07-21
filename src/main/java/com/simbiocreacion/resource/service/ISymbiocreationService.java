package com.simbiocreacion.resource.service;

import com.simbiocreacion.resource.model.Symbiocreation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Date;

public interface ISymbiocreationService {

    Mono<Symbiocreation> create(Symbiocreation e);

    Mono<Symbiocreation> findById(String id);

    Flux<Symbiocreation> findAll();

    //Flux<Symbiocreation> findByUserId(String userId);

    Flux<Symbiocreation> findByUserId(String userId);

    Flux<Symbiocreation> findAllByUser(String userId);

    Flux<Symbiocreation> findAllByVisibility(String visibility);

    Flux<Symbiocreation> findUpcomingByVisibility(String visibility, Date now);

    Flux<Symbiocreation> findPastByVisibility(String visibility, Date now);

    Mono<Symbiocreation> update(Symbiocreation e);

    Mono<Void> delete(String id);

    Mono<Void> deleteAll();
}
