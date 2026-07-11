package com.simbiocreacion.resource.service;

import com.simbiocreacion.resource.dto.IdeaAiResponse;
import com.simbiocreacion.resource.dto.IdeaRequest;
import com.simbiocreacion.resource.dto.TrendAiResponse;
import com.simbiocreacion.resource.model.Node;
import com.simbiocreacion.resource.model.Symbiocreation;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ILlmService {

    Mono<List<IdeaAiResponse>> getIdeasForSymbioFromLlm(Symbiocreation symbiocreation);

    Mono<List<IdeaAiResponse>> getIdeasForGroupFromLlm(Symbiocreation symbiocreation, Node group);

    /**
     * Genera 3 ideas de inspiración basadas únicamente en el tema de la sesión (name + description),
     * sin requerir ideas existentes. Usado por el flujo "Busco inspiración" cuando no hay ideas todavía.
     * Si la sesión no tiene tema (name y description vacíos), devuelve un aviso con placeholder = true.
     */
    Mono<List<IdeaAiResponse>> getInspirationIdeasFromLlm(Symbiocreation symbiocreation);

    // TODO [Manera recomendada]: Al actualizar Spring AI a una versión que soporte gpt-image-1,
    //  revertir a Mono<Image> y usar ImageModel directamente en vez del WebClient manual.
    Mono<byte[]> getImageFromLlm(IdeaRequest idea);

    Mono<List<TrendAiResponse>> getTrendsForSymbioFromLlm(Symbiocreation symbiocreation);
}
