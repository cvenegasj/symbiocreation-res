package com.simbiocreacion.resource.service;

import com.simbiocreacion.resource.model.Node;
import com.simbiocreacion.resource.model.User;
import com.simbiocreacion.resource.repository.SymbiocreationRepository;
import com.simbiocreacion.resource.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bson.Document;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserService implements IUserService {

    private final UserRepository userRepository;

    private final SymbiocreationRepository symbiocreationRepository;

    @Override
    public Mono<User> create(User e) {
        e.setCreationDateTime(new Date());
        return userRepository.save(e);
    }

    @Override
    public Mono<User> findById(String id) {
        return userRepository.findById(id);
    }

    @Override
    public Flux<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public Flux<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public Mono<User> update(User e) {
        return userRepository.save(e);
    }

    @Override
    public Mono<Void> delete(String id) {
        return userRepository.deleteById(id);
    }

    @Override
    public Mono<Void> deleteAll() {
        return userRepository.deleteAll();
    }

    @Override
    public Mono<Long> count() {
        return userRepository.count();
    }

    @Override
    public Flux<Document> groupAndCountByDate() {
        return userRepository.groupAndCountByDate();
    }

    @Override
    public Flux<Document> getTopUsers() {
//        return userRepository.findByEmail("cvj_2311@hotmail.com")
//                .flatMap(this::computeRelevanceMetric);

        return userRepository.findAll()
                .flatMap(this::computeRelevanceMetric)
                .sort((d1, d2) -> d2.getInteger("relevanceMetric") - d1.getInteger("relevanceMetric")) // DESC order
                .take(10); // top 10
    }

    private Mono<Document> computeRelevanceMetric(User user) {
        Document document = new Document();
        document.put("user", user);

        final int coefSymbiosAsParticipant = 1;
        final int coefAmbassadorScore = 2;

        Mono<Integer> userScoreMono = this.symbiocreationRepository.findAllByUserWithGraphs(user.getId())
                .parallel()
                .map(s -> {
                    final Set<Node> roots = SymbiocreationService.findTreeRoots(s);
                    final Map<Node, Node> leafToRootMap = new HashMap<>();

                    return roots.stream()
                            .parallel()
                            .flatMap(root ->
                                    SymbiocreationService.findLeavesOf(root, new HashSet<>()).stream()
                                            .filter(leaf -> leaf.getU_id() != null &&
                                                    leaf.getU_id().equals(user.getId()))
                                            .map(leaf -> {
                                                leafToRootMap.put(leaf, root);
                                                return leaf;
                                            })
                            )
                            .mapToInt(leaf -> leaf.getRole() != null && leaf.getRole().equals("ambassador") ?
                                    coefAmbassadorScore *
                                            SymbiocreationService.computeDepthOf(leaf, leafToRootMap.get(leaf)) :
                                    coefSymbiosAsParticipant *
                                            SymbiocreationService.computeDepthOf(leaf, leafToRootMap.get(leaf)))
                            .sum();
                })
                .reduce(Integer::sum);
//                .log();

        return Mono.create(callback ->
                userScoreMono.subscribe(score -> {
                    document.put("relevanceMetric", score);

                    callback.success(document);
                })
        );
    }
}
