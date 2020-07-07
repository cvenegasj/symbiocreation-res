package com.simbiocreacion.resource.controller;

import com.simbiocreacion.resource.model.Symbiocreation;
import com.simbiocreacion.resource.service.SymbiocreationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;

//@Controller
@RequiredArgsConstructor
public class SocketController {
/*
    private final SymbiocreationService symbioService;
    private EmitterProcessor<Symbiocreation> processor = EmitterProcessor.create();

    @MessageMapping("listen.symbio")
    public Flux<Symbiocreation> emitSymbioUpdates(String id) {
        return this.processor
                .share()
                .filter(s -> s.getId().equals(id));
    }
*/
}
