package com.simbiocreacion.resource.repository;

import com.simbiocreacion.resource.model.Symbiocreation;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import reactor.core.publisher.Flux;

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
}
