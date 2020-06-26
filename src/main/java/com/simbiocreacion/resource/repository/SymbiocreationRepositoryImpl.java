package com.simbiocreacion.resource.repository;

import com.simbiocreacion.resource.model.Symbiocreation;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import reactor.core.publisher.Flux;
import static org.springframework.data.mongodb.core.aggregation.ArrayOperators.Filter.filter;
import static org.springframework.data.mongodb.core.aggregation.ComparisonOperators.Eq.valueOf;

public class SymbiocreationRepositoryImpl implements SymbiocreationRepositoryCustom {

    private final ReactiveMongoTemplate rxMongoTemplate;

    @Autowired
    public SymbiocreationRepositoryImpl(ReactiveMongoTemplate rxMongoTemplate) {
        this.rxMongoTemplate = rxMongoTemplate;
    }

    @Override
    public Flux<Symbiocreation> findByUserIdSingle(String userId) {
        //LookupOperation lookup = Aggregation.lookup("user", "participants.user", "_id", "");

        MatchOperation matchStage = Aggregation.match(new Criteria("participants.u_id").is(userId));
        ProjectionOperation projectStage = Aggregation.project("name", "lastModified", "enabled", "visibility")
                .and(filter("participants")
                    .as("participant")
                    .by(valueOf("participant.u_id").equalToValue(userId))
                    ).as("participants")
                .and("participants")
                    .size()
                    .as("nParticipants");
        SortOperation sortStage = Aggregation.sort(Sort.Direction.DESC, "lastModified");

        TypedAggregation<Symbiocreation> aggregation = Aggregation.newAggregation(Symbiocreation.class, matchStage, projectStage, sortStage);
        return this.rxMongoTemplate.aggregate(aggregation, Symbiocreation.class);
    }
}
