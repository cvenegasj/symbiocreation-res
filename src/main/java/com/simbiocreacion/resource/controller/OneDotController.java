package com.simbiocreacion.resource.controller;

import com.simbiocreacion.resource.model.OneDot;
import com.simbiocreacion.resource.model.OneDotParticipant;
import com.simbiocreacion.resource.model.User;
import com.simbiocreacion.resource.service.IOneDotService;
import com.simbiocreacion.resource.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxProcessor;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.Map;

@RestController
//@RequiredArgsConstructor
@Slf4j
public class OneDotController {

    private final IOneDotService oneDotService;
    private final IUserService userService;
//    private final Sinks.Many<OneDot> oneDotSink;

    private final FluxProcessor<OneDot, OneDot> oneDotProcessor;
    private final FluxSink oneDotSink;

    public OneDotController(IOneDotService oneDotService, IUserService userService,
                            FluxProcessor<OneDot, OneDot> oneDotProcessor) {
        this.oneDotService = oneDotService;
        this.userService = userService;
        this.oneDotProcessor = oneDotProcessor;
        this.oneDotSink = this.oneDotProcessor.sink();
    }

    @PostMapping("/onedot")
    public Mono<OneDot> create(@RequestBody OneDot oneDot) {
        oneDot.setLastModifiedAt(new Date());
        oneDot.setCreatedAt(new Date());

        return oneDotService.create(oneDot);
    }

    @GetMapping("/onedot/{id}")
    public Mono<OneDot> findById(@PathVariable String id) {
        return oneDotService.findById(id)
                .flatMap(this::completeUsers);
    }

    @GetMapping("/onedot/getMine/{userId}/{page}")
    public Flux<OneDot> findByUserId(@PathVariable String userId, @PathVariable int page) {
        Pageable paging = PageRequest.of(page, 12);
        return oneDotService.findAllByUser(userId, paging);
    }

    @GetMapping("/onedot/countByUser/{userId}")
    public Mono<Long> countOneDotsByUser(@PathVariable String userId) {
        return oneDotService.countByUser(userId);
    }

    @PatchMapping("/onedot/{id}/updateGrid")
    public Mono<Void> updateGridStatus(@PathVariable String id, @RequestBody OneDot newOneDot) {
        return oneDotService.findById(id)
                .flatMap(oneDot -> {
                    oneDot.setLastModifiedAt(new Date());
                    oneDot.setGrid(newOneDot.getGrid());
                    return oneDotService.update(oneDot);
                })
                .flatMap(this::completeUsers)
                .doOnNext(oneDot -> {
//                    Sinks.EmitResult result = oneDotSink.tryEmitNext(oneDot);
                    oneDotSink.next(oneDot);
                })
                .then();
    }

    @PostMapping("/onedot/{id}/createParticipant")
    public Mono<OneDot> createParticipant(@PathVariable String id, @RequestBody OneDotParticipant p) {
        return this.oneDotService.findById(id)
                .flatMap(oneDot -> {
                    oneDot.getParticipants().add(p);
                    oneDot.setLastModifiedAt(new Date());

                    return this.oneDotService.update(oneDot);
                })
                .flatMap(this::completeUsers)
                .doOnNext(oneDotSink::next);
    }

    private Mono<OneDot> completeUsers(OneDot o) {
        Mono<Map<String, User>> monoOfMap = Flux.just(o.getParticipants())
                .flatMapIterable(participants -> participants)
                .flatMap(p -> this.userService.findById(p.getU_id()))
                .collectMap(
                        item -> item.getId(), // key
                        item -> item // value
                );

        return Mono.create(callback -> {
            monoOfMap.subscribe(map -> {
                for (OneDotParticipant p : o.getParticipants()) {
                    p.setUser(map.get(p.getU_id()));
                }
                callback.success(o);
            });
        });
    }
}
