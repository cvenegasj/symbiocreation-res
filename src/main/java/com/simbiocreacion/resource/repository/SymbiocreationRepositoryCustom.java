package com.simbiocreacion.resource.repository;

import com.simbiocreacion.resource.model.Symbiocreation;
import reactor.core.publisher.Flux;

public interface SymbiocreationRepositoryCustom {

    Flux<Symbiocreation> findByUserId(String userId);

}
