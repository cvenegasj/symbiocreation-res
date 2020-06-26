package com.simbiocreacion.resource.repository;

import com.simbiocreacion.resource.model.Symbiocreation;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface SymbiocreationRepository extends ReactiveMongoRepository<Symbiocreation, String>, SymbiocreationRepositoryCustom {

    @Query(value = "{'participants.u_id': ?0}", fields = "{'graph': 0}")
    Flux<Symbiocreation> findByUserId(String userId); // must be sorted by lastModified; only logged-in participant is needed
}
