package com.simbiocreacion.resource.controller;

import com.simbiocreacion.resource.model.Symbiocreation;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

@Controller
@RequiredArgsConstructor
public class RSocketController {

    @MessageMapping("listen.symbio.{id}")
    public Flux<Symbiocreation> emitSymbioUpdates(@DestinationVariable String id) {
        return SymbiocreationController.processor
                .share()
                .filter(s -> s.getId().equals(id));
    }
}
