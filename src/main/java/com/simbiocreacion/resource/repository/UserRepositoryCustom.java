package com.simbiocreacion.resource.repository;

import org.bson.Document;
import reactor.core.publisher.Flux;

public interface UserRepositoryCustom {

    Flux<Document> groupAndCountByDate();
}
