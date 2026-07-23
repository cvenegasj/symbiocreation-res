package com.simbiocreacion.resource.repository;

import com.simbiocreacion.resource.model.Symbiocreation;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RequiredArgsConstructor
public class SymbiocreationRepositoryImpl implements SymbiocreationRepositoryCustom {

    private final ReactiveMongoTemplate rxMongoTemplate;

    @Override
    public Flux<Symbiocreation> findByUserId(String userId) {
        //LookupOperation lookup = Aggregation.lookup("user", "participants.user", "_id", "");

        MatchOperation matchStage = Aggregation.match(new Criteria("participants.u_id").is(userId));
        ProjectionOperation projectStage = Aggregation.project(
                "name", "participants", "lastModified", "enabled", "visibility",
                    "place", "dateTime", "timeZone", "hasStartTime", "description",
                    "infoUrl", "tags", "extraUrls", "sdgs")
                .and("participants").size().as("nParticipants");
        SortOperation sortStage = Aggregation.sort(Sort.Direction.DESC, "lastModified");

        TypedAggregation<Symbiocreation> aggregation = Aggregation.newAggregation(
                Symbiocreation.class,
                matchStage,
                projectStage,
                sortStage
        );

        return this.rxMongoTemplate.aggregate(aggregation, Symbiocreation.class);
    }

    // ========= Tailored queries for analytics =========

    @Override
    public Flux<Document> groupAndCountByDate() {
        // Parse Date object to string and truncate time part
        ProjectionOperation projectionOperation = Aggregation.project("creationDateTime")
                .and(DateOperators.dateOf("creationDateTime").toString("%Y-%m-%d")).as("date");
        // Group and count by date only (no time)
        GroupOperation groupOperation = Aggregation.group("date")
                .count().as("count");
        // Parse String date back to Date object
        ProjectionOperation projectionOperation2 = Aggregation.project("_id", "count")
                .and(
                        DateOperators.DateFromString.fromStringOf("_id").withFormat("%Y-%m-%d")
                ).as("truncatedDateObject");
        // Sort by truncatedDateObject
        SortOperation sortOperation = Aggregation.sort(Sort.Direction.ASC, "truncatedDateObject");
        // Final projection
        ProjectionOperation projectionOperation3 = Aggregation.project("_id", "count");

        Aggregation aggregation = Aggregation.newAggregation(
                projectionOperation,
                groupOperation,
                projectionOperation2,
                sortOperation,
                projectionOperation3
        );

        return this.rxMongoTemplate.aggregate(aggregation, "symbiocreation", Document.class);
    }

    public Flux<Symbiocreation> findByVisibility(String visibility) {
        MatchOperation matchOperation = Aggregation.match(new Criteria("visibility").is(visibility));
        ProjectionOperation projectionOperation = Aggregation.project("id", "name", "description", "graph")
                .and("participants").size().as("nParticipants");

        TypedAggregation<Symbiocreation> aggregation = Aggregation.newAggregation(
                Symbiocreation.class,
                matchOperation,
                projectionOperation
        );

        return this.rxMongoTemplate.aggregate(aggregation, Symbiocreation.class);
    }

    @Override
    public Flux<Symbiocreation> findPublicFiltered(String visibility, String name, Date from, Date to, Pageable pageable) {
        Query query = buildFilterQuery(visibility, name, from, to).with(pageable);
        query.fields().exclude("graph"); // payload liviano
        return this.rxMongoTemplate.find(query, Symbiocreation.class);
    }

    @Override
    public Mono<Long> countPublicFiltered(String visibility, String name, Date from, Date to) {
        return this.rxMongoTemplate.count(buildFilterQuery(visibility, name, from, to), Symbiocreation.class);
    }

    // Arma el criterio combinando visibility + nombre (opcional) + rango de creationDateTime (from/to, opcionales).
    private Query buildFilterQuery(String visibility, String name, Date from, Date to) {
        List<Criteria> criterias = new ArrayList<>();
        criterias.add(Criteria.where("visibility").is(visibility));

        if (name != null && !name.isBlank()) {
            criterias.add(Criteria.where("name").regex(name, "i"));
        }

        if (from != null || to != null) {
            Criteria dateCriteria = Criteria.where("creationDateTime");
            if (from != null) dateCriteria.gte(from);
            if (to != null) dateCriteria.lte(to);
            criterias.add(dateCriteria);
        }

        return new Query(new Criteria().andOperator(criterias.toArray(new Criteria[0])));
    }
}
