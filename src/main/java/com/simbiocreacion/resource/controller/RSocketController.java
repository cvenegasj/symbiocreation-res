package com.simbiocreacion.resource.controller;

import com.simbiocreacion.resource.model.Symbiocreation;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxProcessor;

@Controller
@RequiredArgsConstructor
public class RSocketController {

    private final FluxProcessor<Symbiocreation, Symbiocreation> symbioProcessor;

    @MessageMapping("listen.symbio.{id}")
    public Flux<Symbiocreation> emitSymbioUpdates(@DestinationVariable String id) {
        return symbioProcessor
                .share()
                .filter(s -> s.getId().equals(id));
    }
}
