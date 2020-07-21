package com.simbiocreacion.resource.controller;

import com.simbiocreacion.resource.model.User;
import com.simbiocreacion.resource.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Log4j2
public class UserController {

    private final UserService userService;

    /*@PostMapping("/users")
    public void create(@RequestBody Mono<User> u) {
        u.subscribe(userService::create);
    } */

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
        return userService.update(u);
    }

    @GetMapping("/users/init")
    public void initDB() {

        Flux<User> saved = Flux.just(
                new User(UUID.randomUUID().toString(), "First User", "First", "User", "first@user.com", "", false),
                new User(UUID.randomUUID().toString(), "Second User", "Second", "User", "second@user.com", "", false)
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

    /*public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }*/
}
