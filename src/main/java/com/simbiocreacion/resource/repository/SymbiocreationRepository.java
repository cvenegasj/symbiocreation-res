package com.simbiocreacion.resource.repository;

import com.simbiocreacion.resource.model.Symbiocreation;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

import java.util.Date;

public interface SymbiocreationRepository extends ReactiveMongoRepository<Symbiocreation, String>, SymbiocreationRepositoryCustom {

    //@Query(value = "{'participants.u_id': ?0}", fields = "{'graph': 0}")
    //Flux<Symbiocreation> findByUserId(String userId); // must be sorted by lastModified; only logged-in participant is needed

    @Query(value = "{'participants.u_id': ?0}", fields = "{'graph': 0}", sort = "{lastModified: -1}") // ineffective to order in backend????
    Flux<Symbiocreation> findAllByUser(String userId);

    @Query(value = "{'visibility': ?0}", fields = "{'graph': 0}", sort = "{lastModified: -1}")
    Flux<Symbiocreation> findAllByVisibility(String visibility);

    @Query(value = "{'visibility': ?0, 'dateTime' : {'$gt' : ?1}}", fields = "{'graph': 0}", sort = "{dateTime: 1}")
    Flux<Symbiocreation> findUpcomingByVisibility(String visibility, Date now);

    @Query(value = "{'visibility': ?0, 'dateTime' : {'$lt' : ?1}}", fields = "{'graph': 0}", sort = "{dateTime: -1}")
    Flux<Symbiocreation> findPastByVisibility(String visibility, Date now);

}
