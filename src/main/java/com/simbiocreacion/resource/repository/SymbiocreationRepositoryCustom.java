package com.simbiocreacion.resource.repository;

import com.simbiocreacion.resource.model.Symbiocreation;
import org.bson.Document;
import reactor.core.publisher.Flux;

public interface SymbiocreationRepositoryCustom {

    Flux<Symbiocreation> findByUserId(String userId);

    Flux<Document> groupAndCountByDate();

    Flux<Symbiocreation> findByVisibility(String visibility);
}
