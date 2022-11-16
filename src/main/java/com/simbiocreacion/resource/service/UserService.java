package com.simbiocreacion.resource.service;

import com.simbiocreacion.resource.model.User;
import com.simbiocreacion.resource.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final UserRepository userRepository; // like the JPA EntityManager wrapper w find-get/save/delete/update operations

    @Override
    public Mono<User> create(User e) {
        e.setCreationDateTime(new Date());
        return userRepository.save(e);
    }

    @Override
    public Mono<User> findById(String id) {
        return userRepository.findById(id);
    }

    @Override
    public Flux<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public Flux<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public Mono<User> update(User e) {
        return userRepository.save(e);
    }

    @Override
    public Mono<Void> delete(String id) {
        return userRepository.deleteById(id);
    }

    @Override
    public Mono<Void> deleteAll() {
        return userRepository.deleteAll();
    }

    @Override
    public Mono<Long> count() {
        return userRepository.count();
    }

    @Override
    public Flux<Document> groupAndCountByDate() {
        return userRepository.groupAndCountByDate();
    }
}
