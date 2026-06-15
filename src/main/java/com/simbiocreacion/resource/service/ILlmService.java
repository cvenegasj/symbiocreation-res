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

    // TODO [Manera recomendada]: Al actualizar Spring AI a una versión que soporte gpt-image-1,
    //  revertir a Mono<Image> y usar ImageModel directamente en vez del WebClient manual.
    Mono<byte[]> getImageFromLlm(IdeaRequest idea);

    Mono<List<TrendAiResponse>> getTrendsForSymbioFromLlm(Symbiocreation symbiocreation);
}
