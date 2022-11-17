package com.simbiocreacion.resource.repository;

import com.simbiocreacion.resource.model.Symbiocreation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Date;

public interface SymbiocreationRepository extends ReactiveMongoRepository<Symbiocreation, String>, SymbiocreationRepositoryCustom {

    @Query(value = "{'participants.u_id': ?0}", fields = "{'graph': 0}", sort = "{lastModified: -1}") // ineffective to order in backend????
    Flux<Symbiocreation> findAllByUser(String userId, Pageable pageable);

    @Query(value = "{'participants.u_id': ?0}")
    Flux<Symbiocreation> findAllByUserWithGraphs(String userId);

    //@Query(value = "{'visibility': ?0}", fields = "{'graph': 0}", sort = "{lastModified: -1}")
    //Flux<Symbiocreation> findAllByVisibility(String visibility);

    // returns one page of all symbiocreations of a given visibility type
    // returns Flux instead of Page<T> because Spring Data does not support Page as return type when using WebFlux
    @Query(fields = "{'graph': 0}")
    Flux<Symbiocreation> findByVisibilityOrderByLastModifiedDesc(String visibility, Pageable pageable);

    //@Query(value = "{'visibility': ?0, 'dateTime' : {'$gt' : ?1}}", fields = "{'graph': 0}", sort = "{dateTime: 1}")
    //Flux<Symbiocreation> findUpcomingByVisibility(String visibility, Date now);

    // Pageable object contains page index, page size, and sorting parameters
    @Query(fields = "{'graph': 0}")
    Flux<Symbiocreation> findByVisibilityAndDateTimeLessThanEqual(String visibility, Date now, Pageable pageable);

    @Query(fields = "{'graph': 0}")
    Flux<Symbiocreation> findByVisibilityAndDateTimeGreaterThanEqual(String visibility, Date now, Pageable pageable);

    Mono<Long> countByVisibility(String visibility);

    Mono<Long> countByVisibilityAndDateTimeLessThanEqual(String visibility, Date dateTime);

    Mono<Long> countByVisibilityAndDateTimeGreaterThanEqual(String visibility, Date dateTime);

    @Query(value = "{'participants.u_id': ?0}", count = true)
    Mono<Long> countByParticipantsU_id(String userId);
}
