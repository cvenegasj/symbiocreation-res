package com.simbiocreacion.resource.repository;

import com.simbiocreacion.resource.model.User;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface UserRepository extends ReactiveMongoRepository<User, String>, UserRepositoryCustom {

    Flux<User> findByEmail(String email);

    Flux<User> findTop10ByOrderByScoreDesc();
}
