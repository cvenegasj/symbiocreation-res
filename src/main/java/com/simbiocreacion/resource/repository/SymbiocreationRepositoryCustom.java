package com.simbiocreacion.resource.repository;

import com.simbiocreacion.resource.model.Symbiocreation;
import org.bson.Document;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Date;

public interface SymbiocreationRepositoryCustom {

    Flux<Symbiocreation> findByUserId(String userId);

    Flux<Document> groupAndCountByDate();

    Flux<Symbiocreation> findByVisibility(String visibility);

    // Trae todas las simbios de una visibilidad con el documento completo (incluye grafo y participantes),
    // para poder rankear en la app por campos derivados (cantidad de ideas, de colaboradores, etc.).
    Flux<Symbiocreation> findAllByVisibilityFull(String visibility);

    // Listado público filtrado dinámicamente: nombre (opcional) + rango de fecha de creación (from/to, opcionales),
    // ordenado por creationDateTime desc (viene en el Pageable). El grafo se excluye para payload liviano.
    Flux<Symbiocreation> findPublicFiltered(String visibility, String name, Date from, Date to, Pageable pageable);

    Mono<Long> countPublicFiltered(String visibility, String name, Date from, Date to);
}
