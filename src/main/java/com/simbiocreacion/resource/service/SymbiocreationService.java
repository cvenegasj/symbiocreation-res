package com.simbiocreacion.resource.service;

import com.simbiocreacion.resource.model.Symbiocreation;
import com.simbiocreacion.resource.repository.SymbiocreationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class SymbiocreationService implements ISymbiocreationService {

    @Autowired
    private SymbiocreationRepository symbioRepository; // like the JPA EntityManager wrapper w find-get/save/delete/update operations

    @Override
    public Mono<Symbiocreation> create(Symbiocreation e) {
        return symbioRepository.save(e); //.subscribe() ??
    }

    @Override
    public Mono<Symbiocreation> findById(String id) {
        return symbioRepository.findById(id);
    }

    @Override
    public Flux<Symbiocreation> findAll() {
        return symbioRepository.findAll();
    }

    @Override
    public Flux<Symbiocreation> findByUserId(String userId) {
        return symbioRepository.findByUserId(userId);
    }

    @Override
    public Flux<Symbiocreation> findByUserIdSingle(String userId) {
        return symbioRepository.findByUserIdSingle(userId);
    }

    @Override
    public Mono<Symbiocreation> update(Symbiocreation e) {
        return symbioRepository.save(e);
    }

    @Override
    public Mono<Void> delete(String id) {
        return symbioRepository.deleteById(id);
    }

    @Override
    public Mono<Void> deleteAll() {
        return symbioRepository.deleteAll();
    }
}
