package com.simbiocreacion.resource.service;

import com.simbiocreacion.resource.model.Symbiocreation;
import com.simbiocreacion.resource.model.User;
import org.bson.Document;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.List;

public interface ISymbiocreationService {

    Mono<Symbiocreation> create(Symbiocreation e);

    Mono<Symbiocreation> findById(String id);

    Flux<Symbiocreation> findAll();

    Flux<Symbiocreation> findByUserId(String userId);

    Flux<Symbiocreation> findAllByUser(String userId, Pageable pageable);

    Flux<Symbiocreation> findByVisibilityOrderByLastModifiedDesc(String visibility, Pageable pageable);

    Flux<Symbiocreation> findByVisibilityAndDateTimeLessThanEqual(String visibility, Date now, Pageable pageable);

    Flux<Symbiocreation> findByVisibilityAndDateTimeGreaterThanEqual(String visibility, Date now, Pageable pageable);

    Mono<Symbiocreation> update(Symbiocreation e);

    Mono<Void> delete(String id);

    Mono<Void> deleteAll();

    Mono<Long> count();

    Mono<Long> countByVisibility(String visibility);

    Mono<Long> countByVisibilityAndDateTimeLessThanEqual(String visibility, Date dateTime);

    Mono<Long> countByVisibilityAndDateTimeGreaterThanEqual(String visibility, Date dateTime);

    Mono<Long> countByUser(String userId);

    Mono<ByteArrayInputStream> generateParticipantsDataCsv(List<User> users);

    Mono<ByteArrayInputStream> generateAllDataCsv(Symbiocreation symbiocreation);

    Flux<Document> groupAndCountByDate();

    Mono<Long> countIdeasAll();
}
