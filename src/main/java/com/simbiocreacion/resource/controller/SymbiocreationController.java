package com.simbiocreacion.resource.controller;

import com.simbiocreacion.resource.model.Node;
import com.simbiocreacion.resource.model.Symbiocreation;
import com.simbiocreacion.resource.model.User;
import com.simbiocreacion.resource.service.SymbiocreationService;
import com.simbiocreacion.resource.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

@RestController
@RequiredArgsConstructor
@Log4j2
public class SymbiocreationController {

    private final SymbiocreationService symbioService;
    private final UserService userService;

    @PostMapping("/symbiocreations")
    public Mono<Symbiocreation> create(@RequestBody Symbiocreation s) {
        return symbioService.create(s);
    }

    @GetMapping("/symbiocreations/{id}")
    public Mono<Symbiocreation> findById(@PathVariable String id) {
        return symbioService.findById(id).flatMap(s -> this.completeUsers(s));
    }

    private Mono<Symbiocreation> completeUsers(Symbiocreation s) {
        List<User> users = new ArrayList<>();
        Flux usersFlux = Flux.just(s.getParticipants())
                .flatMapIterable(participants -> participants)
                .flatMap(p -> this.userService.findById(p.getU_id()))
                .map(u -> users.add(u));

        return Mono.create(callback -> {
            usersFlux.subscribe(
                    res -> log.info(res),
                    err -> log.error(err),
                    () -> {
                        // populate user field in participants
                        s.getParticipants().replaceAll(p -> {
                            for (User u : users) {
                                if (u.getId().equals(p.getU_id())) {
                                    p.setUser(u);
                                    break;
                                }
                            }
                            return p;
                        });

                        // set nodes's ideas to null for a light payload
                        Stack<Node> stack = new Stack<Node>();
                        s.getGraph().forEach(node -> stack.push(node));
                        Node current;

                        while(!stack.isEmpty()) {
                            current = stack.pop();
                            current.setIdea(null);
                            if (current.getChildren() != null) current.getChildren().forEach(child -> stack.push(child));
                        }

                        callback.success(s);
                    });
        });
    }

    private Mono<Node> completeUser(Node n) {
        return Mono.create(callback -> {
            if (n.getU_id() != null) {
                this.userService.findById(n.getU_id()).subscribe(u -> {
                    if (u != null) {
                        n.setUser(u);
                    }
                    callback.success(n);
                });
            } else callback.success(n);
        });
    }

    // TODO: should not receive any param
    @GetMapping("/symbiocreations/getMine/{userId}")
    public Flux<Symbiocreation> findByUserId(@PathVariable String userId) {
        // get user from session
        //userService.findById(fetchedId).flatMap(u -> this.symbioService.findByUserIdSingle(u.getId()));
        return symbioService.findByUserIdSingle(userId);
    }

    @PutMapping("/symbiocreations")
    public Mono<Symbiocreation> update(@RequestBody Symbiocreation s) {
        // s has nodes w/o ideas, complete ideas of these nodes so the DB update does not set ideas as null.
        return this.symbioService.findById(s.getId())
                .map(dbSymbio -> {
                    Stack<Node> stack = new Stack<Node>();
                    s.getGraph().forEach(node -> stack.push(node));
                    Node current;

                    while(!stack.isEmpty()) {
                        current = stack.pop();
                        Node dbNode = traverseAndGetNode(dbSymbio.getGraph(), current.getId());
                        if (dbNode != null) current.setIdea(dbNode.getIdea());

                        if (current.getChildren() != null) current.getChildren().forEach(child -> stack.push(child));
                    }
                    return s;
                })
                .flatMap(sWithIdeas -> this.symbioService.update(sWithIdeas));
    }

    @DeleteMapping("/symbiocreations/{id}")
    public Mono<Void> delete(@PathVariable String id) {
        return this.symbioService.delete(id);
    }

    // Operations with nodes
    @GetMapping("/symbiocreations/{id}/getNode/{nodeId}")
    public Mono<Node> findNodeById(@PathVariable String id, @PathVariable String nodeId) {
        return this.symbioService.findById(id)
                .map(s -> traverseAndGetNode(s.getGraph(), nodeId))
                .flatMap(n -> this.completeUser(n));
    }

    @PutMapping("/symbiocreations/{id}/updateNodeIdea")
    public Mono<Symbiocreation> updateNodeIdea(@PathVariable String id, @RequestBody Node newNode) {
        return this.symbioService.findById(id)
                .map(s -> {
                    s.getGraph().replaceAll(node -> recurseAndReplaceIdea(node, newNode));
                    return s;
                })
                .flatMap(s -> this.symbioService.update(s));
    }

    /*@PutMapping("/symbiocreations/{id}/updateNodeName")
    public Mono<Symbiocreation> updateNodeName(@PathVariable String id, @RequestBody Node newNode) {
        return this.symbioService.findById(id)
                .map(s -> {
                    s.getGraph().replaceAll(node -> recurseAndReplaceName(node, newNode));
                    return s;
                })
                .flatMap(s -> this.symbioService.update(s));
    } */

    private Node recurseAndReplaceIdea(Node node, Node newNode) {
        if (node.getChildren() != null) node.getChildren().forEach(child -> recurseAndReplaceIdea(child, newNode));
        if (node.getId().equals(newNode.getId())) {
            node.setIdea(newNode.getIdea());
        }
        return node;
    }

    /*private Node recurseAndReplaceName(Node node, Node newNode) {
        if (node.getChildren() != null) node.getChildren().forEach(child -> recurseAndReplaceIdea(child, newNode));
        if (node.getId().equals(newNode.getId())) {
            node.setName(newNode.getName());
        }
        return node;
    } */

    private Node traverseAndGetNode(List<Node> nodes, String nodeId) {
        Node n = null;
        Stack<Node> stack = new Stack<Node>();
        nodes.forEach(node -> stack.push(node));
        Node current = nodes.get(0);

        while(!stack.isEmpty()) {
            current = stack.pop();
            if (current.getId().equals(nodeId)) {
                n = current;
                break;
            }
            if (current.getChildren() != null) current.getChildren().forEach(child -> stack.push(child));
        }
        return n;
    }

}
