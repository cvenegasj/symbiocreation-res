package com.simbiocreacion.resource.service;

import com.simbiocreacion.resource.model.OneDot;
import com.simbiocreacion.resource.repository.OneDotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Log4j2
public class OneDotService implements IOneDotService {

    private final OneDotRepository oneDotRepository;

    @Override
    public Mono<OneDot> create(OneDot e) {
        return oneDotRepository.save(e);
    }

    @Override
    public Mono<OneDot> findById(String id) {
        return oneDotRepository.findById(id);
    }

    @Override
    public Flux<OneDot> findAllByUser(String userId, Pageable pageable) {
        return oneDotRepository.findAllByUser(userId, pageable);
    }

    @Override
    public Mono<OneDot> update(OneDot e) { return oneDotRepository.save(e); };

    @Override
    public Mono<Long> countByUser(String userId) {
        return oneDotRepository.countByParticipantsU_id(userId);
    }
}
