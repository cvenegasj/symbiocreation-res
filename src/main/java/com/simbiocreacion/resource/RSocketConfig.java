package com.simbiocreacion.resource;

import org.springframework.boot.autoconfigure.rsocket.RSocketProperties;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.net.URI;

//@Configuration
public class RSocketConfig {
/*
    @LocalServerPort
    private int port;

    @Bean
    public Mono<RSocketRequester> rSocketRequester(
            RSocketStrategies rSocketStrategies,
            RSocketProperties rSocketProps) {
        return RSocketRequester.builder()
                .rsocketStrategies(rSocketStrategies)
                .connectWebSocket(getURI(rSocketProps));
    }

    private URI getURI(RSocketProperties rSocketProps) {
        return URI.create(String.format("ws://localhost:%d%s",
                port, rSocketProps.getServer().getMappingPath()));
    }*/
}
