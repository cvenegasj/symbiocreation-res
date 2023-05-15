package com.simbiocreacion.resource.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Sinks;

@RestController
@RequiredArgsConstructor
@Log4j2
public class OneDotController {

    private final Sinks.Many oneDotSink;


}
