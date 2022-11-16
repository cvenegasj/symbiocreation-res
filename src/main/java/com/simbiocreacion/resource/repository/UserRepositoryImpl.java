package com.simbiocreacion.resource.repository;

import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom {

    private final ReactiveMongoTemplate rxMongoTemplate;

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

        return this.rxMongoTemplate.aggregate(aggregation, "user", Document.class);
    }
}
