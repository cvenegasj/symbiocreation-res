package com.simbiocreacion.resource.controller;

import com.simbiocreacion.resource.model.OneDot;
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
    private final FluxProcessor<OneDot, OneDot> oneDotProcessor;

    @MessageMapping("listen.symbio.{id}")
    public Flux<Symbiocreation> emitSymbioUpdates(@DestinationVariable String id) {
        return symbioProcessor.share()
                .filter(s -> s.getId().equals(id));
    }

//    @MessageMapping("listen.onedot.{id}")
//    public Flux<OneDot> emitOneDotUpdates(@DestinationVariable String id) {
//        return oneDotSink.asFlux()
//                .filter(oneDot -> oneDot.getId().equals(id));
//    }

    @MessageMapping("listen.onedot.{id}")
    public Flux<OneDot> emitOneDotUpdates(@DestinationVariable String id) {
        return oneDotProcessor.share()
                .filter(s -> s.getId().equals(id));
    }
}
