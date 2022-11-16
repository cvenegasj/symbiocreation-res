package com.simbiocreacion.resource.service;

import com.opencsv.CSVWriter;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvException;
import com.simbiocreacion.resource.model.Node;
import com.simbiocreacion.resource.model.Symbiocreation;
import com.simbiocreacion.resource.model.User;
import com.simbiocreacion.resource.repository.SymbiocreationRepository;
import com.simbiocreacion.resource.util.ByteArrayInOutStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bson.Document;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class SymbiocreationService implements ISymbiocreationService {

    private final IUserService userService;

    private final SymbiocreationRepository symbioRepository; // like the JPA EntityManager wrapper w find-get/save/delete/update operations

    @Override
    public Mono<Symbiocreation> create(Symbiocreation e) {
        return symbioRepository.save(e); //.subscribe() ??
    }

    @Override
    public Mono<Symbiocreation> findById(String id) {
        return symbioRepository.findById(id);
    }

    @Override
    public Flux<Symbiocreation> findAll() {
        return symbioRepository.findAll();
    }

    @Override
    public Flux<Symbiocreation> findByUserId(String userId) {
        return symbioRepository.findByUserId(userId);
    }

    @Override
    public Flux<Symbiocreation> findAllByUser(String userId, Pageable pageable) {
        return symbioRepository.findAllByUser(userId, pageable);
    }

    @Override
    public Flux<Symbiocreation> findByVisibilityOrderByLastModifiedDesc(String visibility, Pageable pageable) {
        return symbioRepository.findByVisibilityOrderByLastModifiedDesc(visibility, pageable);
    }

    @Override
    public Flux<Symbiocreation> findByVisibilityAndDateTimeLessThanEqual(String visibility, Date now, Pageable pageable) {
        return symbioRepository.findByVisibilityAndDateTimeLessThanEqual(visibility, now, pageable);
    }

    @Override
    public Flux<Symbiocreation> findByVisibilityAndDateTimeGreaterThanEqual(String visibility, Date now, Pageable pageable) {
        return symbioRepository.findByVisibilityAndDateTimeGreaterThanEqual(visibility, now, pageable);
    }

    @Override
    public Mono<Symbiocreation> update(Symbiocreation e) {
        return symbioRepository.save(e);
    }

    @Override
    public Mono<Void> delete(String id) {
        return symbioRepository.deleteById(id);
    }

    @Override
    public Mono<Void> deleteAll() {
        return symbioRepository.deleteAll();
    }

    @Override
    public Mono<Long> count() {
        return symbioRepository.count();
    }

    @Override
    public Mono<Long> countByVisibility(String visibility) {
        return symbioRepository.countByVisibility(visibility);
    }

    @Override
    public Mono<Long> countByVisibilityAndDateTimeLessThanEqual(String visibility, Date dateTime) {
        return symbioRepository.countByVisibilityAndDateTimeLessThanEqual(visibility, dateTime);
    }

    @Override
    public Mono<Long> countByVisibilityAndDateTimeGreaterThanEqual(String visibility, Date dateTime) {
        return symbioRepository.countByVisibilityAndDateTimeGreaterThanEqual(visibility, dateTime);
    }

    @Override
    public Mono<Long> countByUser(String userId) {
        return symbioRepository.countByParticipantsU_id(userId);
    }

    @Override
    public Flux<Document> groupAndCountByDate() {
        return symbioRepository.groupAndCountByDate();
    }

    @Override
    public Mono<Long> countIdeasAll() {

        return symbioRepository.findAll()
                .parallel()
                .map(this::countIdeasInSymbiocreation)
                .reduce(Long::sum);
    }

    private Long countIdeasInSymbiocreation(Symbiocreation symbiocreation) {

        return symbiocreation.getGraph().stream()
                .parallel()
                .mapToLong(this::countIdeasInTree)
                .sum();
    }

    private Long countIdeasInTree(Node node) {
        long count = 0;

        if (node.getIdea() != null) {
            count++;
        }

        if (node.getChildren() != null) {
            for (Node child : node.getChildren()) {
                count += countIdeasInTree(child);
            }
        }

        return count;
    }

    // CSV writer
    public Mono<ByteArrayInputStream> generateParticipantsDataCsv(List<User> users) {
        String[] columns = {"Id", "Name", "FirstName", "LastName", "Email"};

        return Mono.fromCallable(() -> {
            try {
                ByteArrayInOutStream stream = new ByteArrayInOutStream();
                OutputStreamWriter streamWriter = new OutputStreamWriter(stream);
                CSVWriter writer = new CSVWriter(streamWriter);

                ColumnPositionMappingStrategy mappingStrategy = new ColumnPositionMappingStrategy();
                mappingStrategy.setType(User.class);
                mappingStrategy.setColumnMapping(columns);
                writer.writeNext(columns);

                StatefulBeanToCsv beanToCsv = new StatefulBeanToCsvBuilder(writer)
                        .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                        .withMappingStrategy(mappingStrategy)
                        .withSeparator(',')
                        .build();
                beanToCsv.write(users);

                streamWriter.flush();

                return stream.getInputStream();
            } catch (CsvException | IOException e) {
                throw new RuntimeException(e);
            }

        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<ByteArrayInputStream> generateAllDataCsv(Symbiocreation symbiocreation) {
        final Set<Node> roots = this.findTreeRoots(symbiocreation); // find roots of symbio object (can have many trees)
//        System.out.println("root found: " + roots.size());
//        System.out.println("roots: " + roots.stream().map(Node::getName).collect(Collectors.toSet()));

        final Set<Node> singles = this.findSingleNodes(symbiocreation); // find single nodes
//        System.out.println("singles found: " + singles.size());
//        System.out.println("singles: " + singles.toString());

        // write CSV content
        return Mono.fromCallable(() -> {
            try {
                ByteArrayInOutStream stream = new ByteArrayInOutStream();
                OutputStreamWriter streamWriter = new OutputStreamWriter(stream);
                CSVWriter writer = new CSVWriter(streamWriter);

                // Write Symbio info
                String[] lineData = {"Simbiocreación"};
                writer.writeNext(lineData);

                lineData = new String[] {"ID", symbiocreation.getId()};
                writer.writeNext(lineData);

                lineData = new String[] {"Nombre", symbiocreation.getName()};
                writer.writeNext(lineData);

                lineData = new String[] {"Descripción", symbiocreation.getDescription()};
                writer.writeNext(lineData);

                lineData = new String[] {"Lugar", symbiocreation.getPlace() != null ?
                        symbiocreation.getPlace() : ""};
                writer.writeNext(lineData);

                lineData = new String[] {"Fecha", symbiocreation.getDateTime() != null ?
                        symbiocreation.getDateTime().toString() : ""};
                writer.writeNext(lineData);

                lineData = new String[] {"Número de participantes", String.valueOf(symbiocreation.getParticipants().size())};
                writer.writeNext(lineData);

                writer.writeNext(new String[] {}); // empty line

                lineData = new String[] {"Participantes"};
                writer.writeNext(lineData);

                for (var participant : symbiocreation.getParticipants()) {
                    lineData = new String[] {participant.getUser().getName(), participant.getUser().getEmail()};
                    writer.writeNext(lineData);
                }

                writer.writeNext(new String[] {}); // empty line
                writer.writeNext(new String[] {}); // empty line

                lineData = new String[] {"Grafo"};
                writer.writeNext(lineData);

                // Tree roots
                for (Node root : roots) {
                    writer.writeNext(new String[] {"=============="});

                    final List<Set<Node>> levels = computeTreeLevels(root);
                    final Map<Node, Set<Node>> nodesLeaves = findNodesLeaves(root); // participants of each node
                    //nodesLeaves.forEach((k, v) -> System.out.println(k.getName() + ": " + v));

                    for (int i = levels.size() - 1; i >= 0; i--) {
                        // write level header with reversed numbering
                        String[] levelHeader = {"Nivel " + String.valueOf(levels.size() - i - 1)};
                        writer.writeNext(levelHeader);

                        for (Node node : levels.get(i)) {
                            // write node name and idea
                            if (i == levels.size() - 1) { // level with leaves (participants)
                                node.setUser(userService.findById(node.getU_id()).block()); //TODO user could be null ?

                                String[] line = {
                                        node.getUser().getName(),
                                        node.getUser().getEmail(),
                                        node.getIdea() != null ? node.getIdea().getTitle() : "",
                                        node.getIdea() != null ? node.getIdea().getDescription() : ""
                                };
                                writer.writeNext(line);
                            } else { // level with group nodes
                                String[] line = {"Grupo: " + node.getName()};
                                writer.writeNext(line);

                                line = new String[] {
                                        "",
                                        "Idea",
                                        node.getIdea() != null ? node.getIdea().getTitle() : ""
                                };
                                writer.writeNext(line);

                                line = new String[] {
                                        "",
                                        "Descripción",
                                        node.getIdea() != null ? node.getIdea().getDescription() : ""
                                };
                                writer.writeNext(line);

                                // write participants of the group
                                line = new String[] {"", "Miembros"};
                                writer.writeNext(line);

                                for (var leaf : nodesLeaves.get(node)) {
                                    line = new String[] {
                                            "",
                                            "",
                                            leaf.getUser() != null ? leaf.getUser().getName() : "",
                                            leaf.getUser() != null ? leaf.getUser().getEmail() : ""};
                                    writer.writeNext(line);
                                }
                            }

                            if (i < levels.size() - 1) {
                                writer.writeNext(new String[] {}); // empty line
                            }
                        }

                        if (i == levels.size() - 1) {
                            writer.writeNext(new String[] {}); // empty line
                        }
                    }

                    writer.writeNext(new String[] {}); // empty line
                }

                //writer.writeNext(new String[] {}); // empty line
                if (!singles.isEmpty()) {
                    writer.writeNext(new String[] {"=============="});
                }

                // Single Nodes
                for (Node node : singles) {
                    if (node.getUser() == null) continue;

                    node.setUser(userService.findById(node.getU_id()).block());
                    lineData = new String[] {
                            node.getUser().getName(),
                            node.getUser().getEmail(),
                            node.getIdea() != null ? node.getIdea().getTitle() : "",
                            node.getIdea() != null ? node.getIdea().getDescription() : ""};
                    writer.writeNext(lineData);
                }

                streamWriter.flush();

                return stream.getInputStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public List<Set<Node>> computeTreeLevels(Node root) {
        final List<Set<Node>> levels = new ArrayList<>();

        // BFS
        Queue<Node> queue = new LinkedList<>();
        queue.add(root);
        Map<Node, Integer> nodeLevels = new HashMap<>();
        nodeLevels.put(root, 0);

        int level = 0;
        while (!queue.isEmpty()) {
            Node curr = queue.remove();

            int currLevel = nodeLevels.get(curr);
            if (currLevel + 1 > levels.size()) {
                levels.add(new HashSet<>());
                level++;
            }
            levels.get(currLevel).add(curr);

            if (curr.getChildren() == null) continue;

            for (Node child : curr.getChildren()) {
                nodeLevels.put(child, level);
                queue.add(child);
            }
        }

        return levels;
    }

    private Map<Node, Set<Node>> findNodesLeaves(Node root) {
        final Map<Node, Set<Node>> leaves = new HashMap<>();
        findParticipantsForTreeDFS(root, leaves);

        return leaves;
    }

    private void findParticipantsForTreeDFS(Node node, Map<Node, Set<Node>> leaves) {
        leaves.put(node, new HashSet<>());

        if (node.getChildren() == null) return;

        node.getChildren().forEach(child -> {
            findParticipantsForTreeDFS(child, leaves);

            if (child.getChildren() == null) {
                leaves.get(node).add(child);
            } else {
                leaves.get(node).addAll(leaves.get(child));
            }
        });
    }

    private Set<Node> findTreeRoots(Symbiocreation symbio) {
        return symbio.getGraph().stream()
                .filter(node -> node.getChildren() != null && !node.getChildren().isEmpty())
                .collect(Collectors.toSet());
    }

    private Set<Node> findSingleNodes(Symbiocreation symbio) {
        return symbio.getGraph().stream()
                .filter(node -> node.getChildren() == null || node.getChildren().isEmpty())
                .collect(Collectors.toSet());
    }
}
