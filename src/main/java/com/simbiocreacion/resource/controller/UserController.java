package com.simbiocreacion.resource.controller;

import com.simbiocreacion.resource.model.Role;
import com.simbiocreacion.resource.model.User;
import com.simbiocreacion.resource.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Log4j2
public class UserController {

    private final UserService userService;

    @PostMapping("/users")
    public Mono<User> create(@RequestBody User u) {
        return userService.create(u);
    }

    @GetMapping("/users/{id}")
    public Mono<ResponseEntity<User>> findById(@PathVariable String id) {
          return userService.findById(id)
                  .map(user -> ResponseEntity.ok(user))
                  .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/users/getByEmail/{email}")
    public Mono<User> findByEmail(@PathVariable String email) {
        return userService.findByEmail(email).next();
    }

    @GetMapping("/users")
    public Flux<User> findAll() {
        return userService.findAll();
    }

    @PutMapping("/users")
    public Mono<User> update(@RequestBody User u) {

        return this.userService.recomputeScore(u.getId())
                .flatMap(userService::update);
    }

    @PatchMapping("/users/{userId}/recompute-score")
    public Mono<User> recomputeUserScore(@PathVariable String userId) {
        return this.userService.recomputeScore(userId);
    }

    @GetMapping("/users/init")
    public void initDB() {
        Flux<User> saved = Flux.just(
                new User(
                        UUID.randomUUID().toString(), "First User", "First",
                        "User", "first@user.com", "", false, Role.USER.toString(),
                        new Date(), 0),
                new User(UUID.randomUUID().toString(), "Second User", "Second",
                        "User", "second@user.com", "", false, Role.USER.toString(),
                        new Date(), 0)
        ).flatMap(userService::create);

        userService.deleteAll()
                .thenMany(saved)
                .thenMany(userService.findAll())
                .subscribe(log::info);

        // fetch all customers
        /*System.out.println("Users found with findAll():");
        System.out.println("-------------------------------");
        userService.findAll()
                .map(user -> user.getFirstName())
                .subscribe(name -> System.out.println(name));*/
    }

    /*
    @GetMapping("/users/remodel")
    public void remodel() {
        symbioService.findAll()
                .flatMap(s -> {
                    //this.recurse(s.getGraph());
                    s.getParticipants().forEach(p -> {
                        if (p.getRole() != null)
                            p.setIsModerator(p.getRole().equals("moderator") ? true : false);
                        else p.setIsModerator(false);
                    });
                    return symbioService.update(s);
                }).subscribe();
    }*/

    // imgPublicId -> imgPublicIds
    /*private void recurse(List<Node> nodes) {
        nodes.forEach(n -> {
            Idea idea = n.getIdea();
            if (idea != null && idea.getImgPublicId() != null) {
                n.getIdea().setImgPublicIds(Arrays.asList(n.getIdea().getImgPublicId()));
                n.getIdea().setImgPublicId(null);
            }
            if (n.getChildren() != null) this.recurse(n.getChildren());
        });
    } */
}
