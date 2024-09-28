package com.simbiocreacion.resource.service;

import com.simbiocreacion.resource.dto.IdeaAiResponse;
import com.simbiocreacion.resource.dto.IdeaRequest;
import com.simbiocreacion.resource.model.Node;
import com.simbiocreacion.resource.model.Symbiocreation;
import org.springframework.ai.image.Image;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ILlmService {

    Mono<List<IdeaAiResponse>> getIdeasForSymbioFromLlm(Symbiocreation symbiocreation);

    Mono<List<IdeaAiResponse>> getIdeasForGroupFromLlm(Symbiocreation symbiocreation, Node group);

    Mono<Image> getImageFromLlm(IdeaRequest idea);

    Mono<List<String>> getTrendsForSymbioFromLlm(Symbiocreation symbiocreation);
}
