package com.simbiocreacion.resource.controller;

import com.simbiocreacion.resource.model.*;
import com.simbiocreacion.resource.service.ISymbiocreationService;
import com.simbiocreacion.resource.service.IUserService;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
//@RequiredArgsConstructor
@Log4j2
public class SymbiocreationController {

    private final ISymbiocreationService symbioService;
    private final IUserService userService;
    //private final Mono<RSocketRequester> requester;
    static final FluxProcessor<Symbiocreation, Symbiocreation> processor = DirectProcessor.<Symbiocreation>create().serialize();
    static final FluxSink sink = processor.sink();

    public SymbiocreationController(ISymbiocreationService symbioService, IUserService userService) {
        this.symbioService = symbioService;
        this.userService = userService;
        //this.processor = EmitterProcessor.<Symbiocreation>create().serialize();
        //this.sink = this.processor.sink();
    }

    @PostMapping("/symbiocreations")
    public Mono<Symbiocreation> create(@RequestBody Symbiocreation s) {
        s.setLastModified(new Date());
        s.setCreationDateTime(new Date());
        // set id for creator node
        Node nodeCreator = new Node();
        nodeCreator.setId(UUID.randomUUID().toString());

        Participant p = s.getParticipants().get(0); // s has the participant
        nodeCreator.setU_id(p.getU_id());

        if (p.getUser().getName() != null) nodeCreator.setName(p.getUser().getName());
        if (p.getUser().getFirstName() != null && p.getUser().getLastName() != null) nodeCreator.setName(p.getUser().getFirstName() + " " + p.getUser().getLastName());

        s.getGraph().add(nodeCreator);
        return symbioService.create(s);
    }

    @GetMapping("/symbiocreations/{id}")
    public Mono<Symbiocreation> findById(@PathVariable String id) {
        return symbioService.findById(id)
                .flatMap(this::completeUsers)
                .flatMap(this::removeIdeas);
    }

    private Mono<Symbiocreation> completeUsers(Symbiocreation s) {
        Mono<Map<String, User>> monoOfMap = Flux.just(s.getParticipants())
                                    .flatMapIterable(participants -> participants)
                                    .flatMap(p -> this.userService.findById(p.getU_id()))
                                    .collectMap(
                                            item -> item.getId(), // key
                                            item -> item // value
                                    );

        return Mono.create(callback -> {
            monoOfMap.subscribe(map -> {
                for (Participant p : s.getParticipants()) {
                    p.setUser(map.get(p.getU_id()));
                }
                callback.success(s);
            });
        });
    }

    // completes users in comments of idea
    private Mono<Node> completeUsersInComments(Node n) {
        if (n.getIdea() == null || n.getIdea().getComments() == null || n.getIdea().getComments().isEmpty()) {
            return Mono.just(n);
        }

        Mono<Map<String, User>> monoOfMap = Flux.just(n.getIdea().getComments())
                .flatMapIterable(comments -> comments)
                .flatMap(c -> this.userService.findById(c.getU_id()))
                .collectMap(
                        item -> item.getId(),
                        item -> item
                );

        return Mono.create(callback -> {
            monoOfMap.subscribe(
                    map -> {
                        for (Comment comment : n.getIdea().getComments()) {
                            comment.setAuthor(map.get(comment.getU_id()));
                        }
                        callback.success(n);
                    }
            );
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

    private Mono<Comment> completeUser(Comment c) {
        return Mono.create(callback -> {
            this.userService.findById(c.getU_id()).subscribe(u -> {
                if (u != null) {
                    c.setAuthor(u);
                }
                callback.success(c);
            });
        });
    }

    // removes ideas from each node but leaves the title
    private Mono<Symbiocreation> removeIdeas(Symbiocreation s) {
        return Mono.create(callback -> {
            // set nodes's ideas to null for a light payload
            Stack<Node> stack = new Stack<>();
            s.getGraph().forEach(node -> stack.push(node));
            Node current;

            while(!stack.isEmpty()) {
                current = stack.pop();
                if (current.getIdea() != null) {
                    current.getIdea().setDescription(null);
                    current.getIdea().setLastModified(null);
                    current.getIdea().setImgPublicIds(null);
                    current.getIdea().setExternalUrls(null);
                    current.getIdea().setComments(null);
                }
                if (current.getChildren() != null) current.getChildren().forEach(child -> stack.push(child));
            }
            callback.success(s);
        });
    }

    // TODO: should not receive any param -> get user from session
    // can we get all the users from all symbios in a map, so as to not make repeated user lookups
    @GetMapping("/symbiocreations/getMine/{userId}/{page}")
    public Flux<Symbiocreation> findByUserId(@PathVariable String userId, @PathVariable int page) {
        Pageable paging = PageRequest.of(page, 12);
        return symbioService.findAllByUser(userId, paging)
                .flatMap(this::completeUsers); // users needed for displaying participants' names in grid or list view
    }

    @GetMapping("/symbiocreations/getAllPublic/{page}")
    public Flux<Symbiocreation> findPublicAll(@PathVariable int page) {
        // Pageable sortedByPriceDescNameAsc =
        //  PageRequest.of(0, 5, Sort.by("price").descending().and(Sort.by("name")));
        Pageable paging = PageRequest.of(page, 20);
        return symbioService.findByVisibilityOrderByLastModifiedDesc("public", paging)
                .flatMap(this::completeUsers);
    }

    @GetMapping("/symbiocreations/getUpcomingPublic/{page}")
    public Flux<Symbiocreation> findPublicUpcoming(@PathVariable int page) {
        Pageable paging = PageRequest.of(page, 20, Sort.by("dateTime").ascending());
        return symbioService.findByVisibilityAndDateTimeGreaterThanEqual("public", new Date(), paging)
                .flatMap(this::completeUsers);
    }

    @GetMapping("/symbiocreations/getPastPublic/{page}")
    public Flux<Symbiocreation> findPublicPast(@PathVariable int page) {
        Pageable paging = PageRequest.of(page, 20, Sort.by("dateTime").descending());
        return symbioService.findByVisibilityAndDateTimeLessThanEqual("public", new Date(), paging)
                .flatMap(this::completeUsers);
    }

    @GetMapping("/symbiocreations/countByUser/{userId}")
    public Mono<Long> countSymbiocreationsByUser(@PathVariable String userId) {
        return symbioService.countByUser(userId);
    }

    @GetMapping("/symbiocreations/countPublic")
    public Mono<Long> countPublicSymbiocreations() {
        return symbioService.countByVisibility("public");
    }

    @GetMapping("/symbiocreations/countPastPublic")
    public Mono<Long> countPastPublicSymbiocreations() {
        return symbioService.countByVisibilityAndDateTimeLessThanEqual("public", new Date());
    }

    @GetMapping("/symbiocreations/countUpcomingPublic")
    public Mono<Long> countUpcomingPublicSymbiocreations() {
        return symbioService.countByVisibilityAndDateTimeGreaterThanEqual("public", new Date());
    }

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

    @PutMapping("/symbiocreations/{id}/updateInfo")
    public Mono<Void> updateInfo(@PathVariable String id, @RequestBody Symbiocreation newSymbio) {
        return this.symbioService.findById(id)
                .flatMap(s -> {
                    s.setName(newSymbio.getName());
                    s.setPlace(newSymbio.getPlace());
                    s.setDateTime(newSymbio.getDateTime());
                    s.setTimeZone(newSymbio.getTimeZone());
                    s.setHasStartTime(newSymbio.getHasStartTime());

                    s.setDescription(newSymbio.getDescription());
                    s.setInfoUrl(newSymbio.getInfoUrl());
                    s.setTags(newSymbio.getTags());
                    s.setExtraUrls(newSymbio.getExtraUrls());
                    s.setSdgs(newSymbio.getSdgs());

                    //s.setEnabled();
                    s.setVisibility(newSymbio.getVisibility());
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
                .flatMap(this::completeUser)
                .flatMap(this::completeUsersInComments);
    }

    @GetMapping("/symbiocreations/{id}/getNodesByUserId/{userId}")
    public Flux<Node> findNodesByUserId(@PathVariable String id, @PathVariable String userId) {
        return this.symbioService.findById(id)
                .map(s -> traverseAndGetNodesByUserId(s.getGraph(), userId))
                .flatMapMany(Flux::fromIterable);
    }

    @PutMapping("/symbiocreations/{id}/updateNodeIdea")
    public Mono<Node> updateNodeIdea(@PathVariable String id, @RequestBody Node newNode) {
        return this.symbioService.findById(id)
                .flatMap(s -> {
                    this.recurseAndReplaceIdea(s.getGraph(), newNode);
                    s.setLastModified(new Date());
                    return this.symbioService.update(s);
                })
                .flatMap(this::completeUsers)
                .doOnNext(this.sink::next)
                .thenReturn(newNode);
    }

    @PutMapping("/symbiocreations/{id}/createCommentOfIdea/{nodeId}")
    public Mono<Comment> createCommentOfIdea(@PathVariable String id, @PathVariable String nodeId, @RequestBody Comment comment) {
        return this.symbioService.findById(id)
                .flatMap(s -> {
                    comment.setLastModified(new Date());
                    this.recurseAndAddComment(s.getGraph(), nodeId, comment);
                    return this.symbioService.update(s);
                })
                .flatMap(s -> this.completeUser(comment));
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

                    if (p.getUser().getName() != null) newNode.setName(p.getUser().getName());
                    if (p.getUser().getFirstName() != null && p.getUser().getLastName() != null) newNode.setName(p.getUser().getFirstName() + " " + p.getUser().getLastName());

                    s.getGraph().add(newNode);
                    s.setLastModified(new Date());
                    return this.symbioService.update(s);
                })
                .flatMap(this::completeUsers)
                .flatMap(this::removeIdeas)
                .doOnNext(this.sink::next);
    }

    @PostMapping("/symbiocreations/{id}/createUserNode")
    public Mono<Symbiocreation> createUserNode(@PathVariable String id, @RequestBody Node n) {
        return this.symbioService.findById(id)
                .flatMap(s -> {
                    n.setId(UUID.randomUUID().toString());
                    s.getGraph().add(n);
                    s.setLastModified(new Date());
                    return this.symbioService.update(s);
                })
                .flatMap(this::completeUsers)
                .flatMap(this::removeIdeas)
                .doOnNext(this.sink::next);
    }

    // creates a parent node for node with id childId
    @PostMapping("/symbiocreations/{id}/createNextLevelGroup/{childId}")
    public Mono<Symbiocreation> createNextLevelGroup(@PathVariable String id, @PathVariable String childId, @RequestBody Node nextLevelNode) {
        return this.symbioService.findById(id)
                .flatMap(s -> {
                    nextLevelNode.setId(UUID.randomUUID().toString());
                    // get child node
                    Node child = this.traverseAndGetNode(s.getGraph(), childId);
                    // set child as 'ambassador' if child is a user node.
                    if (child.getU_id() != null) {
                        child.setRole("ambassador");
                    }
                    // remove child node from graph
                    this.recurseAndDeleteNode(s.getGraph(), childId);
                    // add child node to parent
                    nextLevelNode.getChildren().add(child);
                    s.getGraph().add(nextLevelNode);
                    return this.symbioService.update(s);
                })
                .flatMap(this::completeUsers)
                .flatMap(this::removeIdeas)
                .doOnNext(this.sink::next);
    }

    @GetMapping("/symbiocreations/{id}/setParentNode/{childId}/{parentId}")
    public Mono<Symbiocreation> setParentNode(@PathVariable String id, @PathVariable String childId, @PathVariable String parentId) {
        return this.symbioService.findById(id)
                .flatMap(s -> {
                    int n1 = this.traverseAndCount(s.getGraph());

                    Node child = this.traverseAndGetNode(s.getGraph(), childId);
                    child.setRole("participant");
                    // remove child node from graph
                    this.recurseAndDeleteNode(s.getGraph(), childId);
                    // add child node to parent
                    if (parentId.equals("none")) s.getGraph().add(child);
                    else this.recurseAndAddNodeAsChild(s.getGraph(), child, parentId);

                    int n2 = this.traverseAndCount(s.getGraph());
                    // safe-check (just in case!)
                    // node count of pre and post graphs should match
                    if (n1 == n2) {
                        s.setLastModified(new Date());
                        return this.symbioService.update(s);
                    } else {
                        return Mono.empty();
                    }
                })
                .flatMap(this::completeUsers)
                .flatMap(this::removeIdeas)
                .doOnNext(this.sink::next);
    }

    @DeleteMapping("/symbiocreations/{id}/deleteNode/{nodeId}")
    public Mono<Symbiocreation> deleteNode(@PathVariable String id, @PathVariable String nodeId) {
        return this.symbioService.findById(id)
                .flatMap(s -> {
                    Node toDelete = this.traverseAndGetNode(s.getGraph(), nodeId);
                    this.recurseAndDeleteNode(s.getGraph(), nodeId);
                    // if group node
                    if (toDelete.getChildren() != null) {
                        s.getGraph().addAll(toDelete.getChildren());
                        // all ambassador members lose their roles
                        toDelete.getChildren().forEach(child -> child.setRole("participant"));
                    }
                    s.setLastModified(new Date());
                    return this.symbioService.update(s);
                })
                .flatMap(this::completeUsers)
                .flatMap(this::removeIdeas)
                .doOnNext(this.sink::next);
    }

    @PutMapping("/symbiocreations/{id}/updateNodeName")
    public Mono<Symbiocreation> updateNodeName(@PathVariable String id, @RequestBody Node newNode) {
        return this.symbioService.findById(id)
                .flatMap(s -> {
                    this.recurseAndReplaceName(s.getGraph(), newNode);
                    s.setLastModified(new Date());
                    return this.symbioService.update(s);
                })
                .flatMap(this::completeUsers)
                .flatMap(this::removeIdeas)
                .doOnNext(this.sink::next);
    }

    // set participant as moderator
    @PutMapping("/symbiocreations/{id}/setParticipantAsModerator")
    public Mono<Symbiocreation> setParticipantAsModerator(@PathVariable String id, @RequestBody Participant participant) {
        return this.symbioService.findById(id)
                .flatMap(s -> {
                    for (int i = 0; i < s.getParticipants().size(); i++) {
                        if (s.getParticipants().get(i).getU_id().equals(participant.getU_id())) {
                            s.getParticipants().get(i).setIsModerator(true);
                            break;
                        }
                    }
                    return this.symbioService.update(s);
                })
                .flatMap(this::completeUsers)
                .flatMap(this::removeIdeas)
                .doOnNext(this.sink::next);
    }

    // node contains id and new role
    @PutMapping("/symbiocreations/{id}/updateUserNodeRole")
    public Mono<Symbiocreation> updateUserNodeRole(@PathVariable String id, @RequestBody Node node) {
        return this.symbioService.findById(id)
                .flatMap(s -> {
                    this.recurseAndReplaceRole(s.getGraph(), node);
                    return this.symbioService.update(s);
                })
                .flatMap(this::completeUsers)
                .flatMap(this::removeIdeas)
                .doOnNext(this.sink::next);
    }

    // participant has the new value for isModerator
    @PutMapping("/symbiocreations/{id}/setParticipantIsModerator")
    public Mono<Symbiocreation> updateParticipantIsModerator(@PathVariable String id, @RequestBody Participant participant) {
        return this.symbioService.findById(id)
                .flatMap(s -> {
                    for (int i = 0; i < s.getParticipants().size(); i++) {
                        if (s.getParticipants().get(i).getU_id().equals(participant.getU_id())) {
                            s.getParticipants().get(i).setIsModerator(participant.getIsModerator());
                        }
                    }
                    return this.symbioService.update(s);
                })
                .flatMap(this::completeUsers)
                .flatMap(this::removeIdeas)
                .doOnNext(this.sink::next);
    }

    @DeleteMapping("/symbiocreations/{id}/deleteParticipant/{u_id}")
    public Mono<Symbiocreation> deleteParticipant(@PathVariable String id, @PathVariable String u_id) {
        return this.symbioService.findById(id)
                .flatMap(s -> {
                    // remove participant
                    s.getParticipants().removeIf(p -> p.getU_id().equals(u_id));
                    // remove all nodes associated
                    this.recurseAndDeleteNodesOfUser(s.getGraph(), u_id);
                    return this.symbioService.update(s);
                })
                .flatMap(this::completeUsers)
                .flatMap(this::removeIdeas)
                .doOnNext(this.sink::next);
    }

    @GetMapping("/symbiocreations/{symbioId}/export-participants-data")
    @ResponseBody
    public ResponseEntity<Mono<Resource>> downloadParticipantsData(@PathVariable String symbioId) {
        Mono fetchedContent = symbioService.findById(symbioId)
                .flatMapIterable(s -> s.getParticipants())
                .flatMap(p -> this.userService.findById(p.getU_id()))
                .collect(Collectors.toList())
                .flatMap(users -> this.symbioService.generateParticipantsDataCsv(users))
                .map(InputStreamResource::new);

        String fileName = "participants-data-" + symbioId + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,  "attachment; filename=" + fileName)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .body(fetchedContent);
    }

    @GetMapping("/symbiocreations/{symbioId}/export-all-data")
    @ResponseBody
    public ResponseEntity<Mono<Resource>> downloadAllData(@PathVariable String symbioId) {
        Mono fetchedContent = symbioService.findById(symbioId)
                .flatMap(this::completeUsers)
                .flatMap(this.symbioService::generateAllDataCsv)
                .map(InputStreamResource::new);

        String fileName = "all-data-" + symbioId + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,  "attachment; filename=" + fileName)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .body(fetchedContent);
    }


    /****************** Server-Sent Events **********************/

    @GetMapping(value = "/sse-symbios/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Symbiocreation> streamUpdatesSymbios(@PathVariable String id) {
        //return requester.flatMapMany(r -> r.route("listen.symbio").data("5ef8388cae6aab3379ab9007").retrieveFlux(Symbiocreation.class));
        return this.processor
                //.map(e -> e);
                //.share()
                .filter(s -> s.getId().equals(id));
    }



    /****************** Graph traversal helpers **********************/

    private void recurseAndReplaceIdea(List<Node> nodes, Node newNode) {
        nodes.forEach(n -> {
            if (n.getId().equals(newNode.getId())) {
                n.setIdea(newNode.getIdea());
                return;
            }
            if (n.getChildren() != null) this.recurseAndReplaceIdea(n.getChildren(), newNode);
        });
    }

    private void recurseAndDeleteNode(List<Node> nodes, String nodeId) {
        nodes.forEach(n -> {
            if (n.getChildren() != null) this.recurseAndDeleteNode(n.getChildren(), nodeId);
        });
        nodes.removeIf(node -> node.getId().equals(nodeId));
    }

    private void recurseAndDeleteNodesOfUser(List<Node> nodes, String userId) {
        nodes.forEach(n -> {
            if (n.getChildren() != null) this.recurseAndDeleteNodesOfUser(n.getChildren(), userId);
        });
        nodes.removeIf(node -> node.getU_id() != null && node.getU_id().equals(userId));
    }

    private void recurseAndAddNodeAsChild(List<Node> nodes, Node child, String parentId) {
        nodes.forEach(n -> {
            if (n.getId().equals(parentId)) {
                n.getChildren().add(child);
                return;
            }
            if (n.getChildren() != null) this.recurseAndAddNodeAsChild(n.getChildren(), child, parentId);
        });
    }

    private void recurseAndReplaceName(List<Node> nodes, Node newNode) {
        nodes.forEach(n -> {
            if (n.getId().equals(newNode.getId())) {
                n.setName(newNode.getName());
                return;
            }
            if (n.getChildren() != null) this.recurseAndReplaceName(n.getChildren(), newNode);
        });
    }

    private void recurseAndReplaceRole(List<Node> nodes, Node newNode) {
        nodes.forEach(n -> {
            if (n.getId().equals(newNode.getId())) {
                n.setRole(newNode.getRole());
                return;
            }
            if (n.getChildren() != null) this.recurseAndReplaceRole(n.getChildren(), newNode);
        });
    }

    private void recurseAndAddComment(List<Node> nodes, String nodeId, Comment comment) {
        nodes.forEach(node -> {
            if (node.getId().equals(nodeId)) {
                if (node.getIdea().getComments() == null) node.getIdea().setComments(new ArrayList<>());
                node.getIdea().getComments().add(comment);
                return;
            }
            if (node.getChildren() != null) this.recurseAndAddComment(node.getChildren(), nodeId, comment);
        });
    }

    private int traverseAndCount(List<Node> nodes) {
        int counter = 0;
        Stack<Node> stack = new Stack<Node>();
        nodes.forEach(node -> stack.push(node));
        Node current;

        while (!stack.isEmpty()) {
            current = stack.pop();
            counter++;

            if (current.getChildren() != null) current.getChildren().forEach(child -> stack.push(child));
        }
        return counter;
    }

    private Node traverseAndGetNode(List<Node> nodes, String nodeId) {
        Node n = null;
        Stack<Node> stack = new Stack<>();
        nodes.forEach(stack::push);
        Node current;

        while (!stack.isEmpty()) {
            current = stack.pop();
            if (current.getId().equals(nodeId)) {
                n = current;
                break;
            }
            if (current.getChildren() != null) current.getChildren().forEach(child -> stack.push(child));
        }
        return n;
    }

    private List<Node> traverseAndGetNodesByUserId(List<Node> nodes, String userId) {
        List<Node> result = new ArrayList<>();
        Stack<Node> stack = new Stack<>();
        nodes.forEach(stack::push);
        Node current;

        while (!stack.isEmpty()) {
            current = stack.pop();
            if (current.getU_id() != null && current.getU_id().equals(userId)) {
                result.add(current);
            }
            if (current.getChildren() != null) current.getChildren().forEach(stack::push);
        }
        return result;
    }

    /*
    @GetMapping("/symbiocreations/remodel")
    public void remodel() {
        symbioService.findAll()
                .flatMap(s -> {
                    //this.recurse(s.getGraph()
                    s.getParticipants().forEach(p -> {
                        List<Node> nodes = this.traverseAndGetNodesByUserId(s.getGraph(), p.getU_id());
                        for (Node n : nodes) {
                            if (p.getRole() != null && p.getRole().equals("ambassador")) {
                                n.setRole("ambassador");
                            } else {
                                n.setRole("participant");
                            }
                        }
                    });
                    return symbioService.update(s);
                }).subscribe();
    }*/

}
