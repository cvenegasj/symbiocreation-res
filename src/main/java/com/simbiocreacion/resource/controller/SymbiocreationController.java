package com.simbiocreacion.resource.controller;

import com.simbiocreacion.resource.model.Node;
import com.simbiocreacion.resource.model.Participant;
import com.simbiocreacion.resource.model.Symbiocreation;
import com.simbiocreacion.resource.model.User;
import com.simbiocreacion.resource.service.SymbiocreationService;
import com.simbiocreacion.resource.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

@RestController
//@RequiredArgsConstructor
@Log4j2
public class SymbiocreationController {

    private final SymbiocreationService symbioService;
    private final UserService userService;
    //private final Mono<RSocketRequester> requester;
    private EmitterProcessor<Symbiocreation> processor = EmitterProcessor.create();

    public SymbiocreationController(SymbiocreationService symbioService, UserService userService) {
        this.symbioService = symbioService;
        this.userService = userService;
    }

    @PostMapping("/symbiocreations")
    public Mono<Symbiocreation> create(@RequestBody Symbiocreation s) {
        return symbioService.create(s);
    }

    @GetMapping("/symbiocreations/{id}")
    public Mono<Symbiocreation> findById(@PathVariable String id) {
        return symbioService.findById(id)
                .flatMap(s -> this.completeUsers(s))
                .flatMap(s -> this.removeIdeas(s));
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

    private Mono<Symbiocreation> removeIdeas(Symbiocreation s) {
        return Mono.create(callback -> {
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
    }

    // TODO: should not receive any param
    @GetMapping("/symbiocreations/getMine/{userId}")
    public Flux<Symbiocreation> findByUserId(@PathVariable String userId) {
        // get user from session
        //userService.findById(fetchedId).flatMap(u -> this.symbioService.findByUserIdSingle(u.getId()));
        return symbioService.findByUserIdSingle(userId);
    }

    /*@PutMapping("/symbiocreations")
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
    }*/

    @PutMapping("/symbiocreations/{id}/updateName")
    public Mono<Void> updateName(@PathVariable String id, @RequestBody Symbiocreation newSymbio) {
        return this.symbioService.findById(id)
                .flatMap(s -> {
                    s.setName(newSymbio.getName());

                    s.setLastModified(new Date());

                    return this.symbioService.update(s);
                })
                .then();
                //.flatMap(s -> Mono.empty());
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

                    s.setLastModified(new Date());

                    return s;
                })
                .flatMap(s -> this.symbioService.update(s))
                .flatMap(this::completeUsers)
                .flatMap(this::removeIdeas);
    }

    @PostMapping("/symbiocreations/{id}/createParticipant")
    public Mono<Symbiocreation> createParticipant(@PathVariable String id, @RequestBody Participant p) {
        return this.symbioService.findById(id)
                .flatMap(s -> {
                    // add new participant and node objects
                    s.getParticipants().add(p);

                    Node newNode = new Node();
                    newNode.setId(UUID.randomUUID().toString());
                    newNode.setU_id(p.getU_id());
                    newNode.setName(p.getUser().getFirstName());
                    s.getGraph().add(newNode);

                    s.setLastModified(new Date());

                    return this.symbioService.update(s);
                })
                .flatMap(this::completeUsers)
                .flatMap(this::removeIdeas)
                .doOnNext(this.processor::onNext);
    }

    @PostMapping("/symbiocreations/{id}/createGroupNode")
    public Mono<Symbiocreation> createGroupNode(@PathVariable String id, @RequestBody Node n) {
        return this.symbioService.findById(id)
                .flatMap(s -> {
                    n.setId(UUID.randomUUID().toString());
                    s.getGraph().add(n);

                    s.setLastModified(new Date());

                    return this.symbioService.update(s);
                })
                .flatMap(this::completeUsers)
                .flatMap(this::removeIdeas)
                .doOnNext(this.processor::onNext);
    }

    @GetMapping("/symbiocreations/{id}/setParentNode/{childId}/{parentId}")
    public Mono<Symbiocreation> setParentNode(@PathVariable String id, @PathVariable String childId, @PathVariable String parentId) {
        return this.symbioService.findById(id)
                .flatMap(s -> {
                    Node child = this.traverseAndGetNode(s.getGraph(), childId);
                    // remove child node from graph
                    s.setGraph(this.recurseAndDeleteNode(s.getGraph(), childId));
                    // add child node to parent
                    if (parentId.equals("none")) s.getGraph().add(child);
                    else s.setGraph(this.recurseAndAddNodeAsChild(s.getGraph(), child, parentId));

                    s.setLastModified(new Date());

                    return this.symbioService.update(s);
                })
                .flatMap(this::completeUsers)
                .flatMap(this::removeIdeas)
                .doOnNext(this.processor::onNext);
    }

    @DeleteMapping("/symbiocreations/{id}/deleteNode/{nodeId}")
    public Mono<Symbiocreation> deleteNode(@PathVariable String id, @PathVariable String nodeId) {
        return this.symbioService.findById(id)
                .flatMap(s -> {
                    Node toDelete = this.traverseAndGetNode(s.getGraph(), nodeId);
                    s.setGraph(this.recurseAndDeleteNode(s.getGraph(), nodeId));
                    if (toDelete.getChildren() != null) s.getGraph().addAll(toDelete.getChildren());

                    s.setLastModified(new Date());

                    return this.symbioService.update(s);
                })
                .flatMap(this::completeUsers)
                .flatMap(this::removeIdeas)
                .doOnNext(this.processor::onNext);

    }

    @PutMapping("/symbiocreations/{id}/updateNodeName")
    public Mono<Symbiocreation> updateNodeName(@PathVariable String id, @RequestBody Node newNode) {
        return this.symbioService.findById(id)
                .flatMap(s -> {
                    s.setGraph(this.recurseAndReplaceName(s.getGraph(), newNode));

                    s.setLastModified(new Date());

                    return this.symbioService.update(s);
                })
                .flatMap(this::completeUsers)
                .flatMap(this::removeIdeas)
                .doOnNext(this.processor::onNext);
    }


    /****************** Server-Sent Events **********************/

    @GetMapping(value = "/sse-symbios/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Symbiocreation> streamUpdatesSymbios(@PathVariable String id) {
        //return requester.flatMapMany(r -> r.route("listen.symbio").data("5ef8388cae6aab3379ab9007").retrieveFlux(Symbiocreation.class));
        return this.processor
                .share()
                .filter(s -> s.getId().equals(id));
    }



    /****************** Graph traversal helpers **********************/

    private Node recurseAndReplaceIdea(Node node, Node newNode) {
        if (node.getChildren() != null) node.getChildren().forEach(child -> recurseAndReplaceIdea(child, newNode));
        if (node.getId().equals(newNode.getId())) {
            node.setIdea(newNode.getIdea());
        }
        return node;
    }

    private List<Node> recurseAndDeleteNode(List<Node> nodes, String nodeId) {
        nodes.forEach(n -> {
            if (n.getChildren() != null) n.setChildren(this.recurseAndDeleteNode(n.getChildren(), nodeId));
        });

        nodes.removeIf(node -> node.getId().equals(nodeId));

        return nodes;
    }

    private List<Node> recurseAndAddNodeAsChild(List<Node> nodes, Node child, String parentId) {
        nodes.forEach(n -> {
            if (n.getChildren() != null) n.setChildren(this.recurseAndAddNodeAsChild(n.getChildren(), child, parentId));
            if (n.getId().equals(parentId)) n.getChildren().add(child);
        });

        return nodes;
    }

    private List<Node> recurseAndReplaceName(List<Node> nodes, Node newNode) {
        nodes.forEach(n -> {
            if (n.getChildren() != null) n.setChildren(this.recurseAndReplaceName(n.getChildren(), newNode));
            if (n.getId().equals(newNode.getId())) n.setName(newNode.getName());
        });

        return nodes;
    }

    private Node traverseAndGetNode(List<Node> nodes, String nodeId) {
        Node n = null;
        Stack<Node> stack = new Stack<Node>();
        nodes.forEach(node -> stack.push(node));
        Node current;

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
