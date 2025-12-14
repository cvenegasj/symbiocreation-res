package com.simbiocreacion.resource.service;

import com.simbiocreacion.resource.dto.IdeaAiResponse;
import com.simbiocreacion.resource.dto.IdeaRequest;
import com.simbiocreacion.resource.dto.TrendAiResponse;
import com.simbiocreacion.resource.model.Idea;
import com.simbiocreacion.resource.model.Node;
import com.simbiocreacion.resource.model.Symbiocreation;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageMessage;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LlmService implements ILlmService {

    private final ChatClient chatClient;
    private final ImageModel imageModel;

    @Value("classpath:/prompts/system-for-symbio-ideas.md")
    private Resource systemForSymbioIdeas;

    @Value("classpath:/prompts/system-for-group-ideas.md")
    private Resource systemForGroupIdeas;

    @Value("classpath:/prompts/user-for-symbio-trends.st")
    private Resource userForSymbioTrends;

    private static final String USER_PROBLEM_TEMPLATE_1 = """
            This is the topic you need to address:
            {symbiocreationName}
            {symbiocreationDescription}
            
            """;

    private static final String USER_PROBLEM_TEMPLATE_2 = """
            This is the session's topic:
            {symbiocreationName}
            {symbiocreationDescription}
            
            """;
    private static final String USER_IDEA_TEMPLATE = """
            Título: %s
            Descripción: %s
            
            """;
    private static final String USER_QUERY_TEMPLATE_1 = """
            Give me three new ideas for the topic given to you.
            {format}
            """;

    private static final String USER_QUERY_TEMPLATE_2 = """
            Give me a new idea that summarizes all the ideas given to you.
            {format}
            """;

    private static final Predicate<Idea> TITLE_OR_DESCRIPTION_EXISTS_PREDICATE = idea ->
            (Objects.nonNull(idea.getTitle()) && !idea.getTitle().trim().isEmpty())
                    || (Objects.nonNull(idea.getDescription()) && !idea.getDescription().trim().isEmpty());

    public LlmService(ChatClient.Builder builder, ImageModel imageModel) {
        this.chatClient = builder.build();
        this.imageModel = imageModel;
    }

    @Override
    public Mono<List<IdeaAiResponse>> getIdeasForSymbioFromLlm(Symbiocreation symbiocreation) {
        List<Idea> existingIdeas = getIdeasFromSymbiocreation(symbiocreation, 3);

        if (existingIdeas.isEmpty()) {
            return Mono.just(List.of(new IdeaAiResponse(
                    "Se necesitan más ideas",
                    "Para usar esta funcionalidad, los participantes deben agregar al menos una idea a la sesión. " +
                    "Una vez que haya ideas disponibles, la IA podrá generar nuevas sugerencias basadas en ellas."
            )));
        }

        return Mono.fromCallable(() -> {
            PromptTemplate userProblemPromptTemplate = new PromptTemplate(
                    USER_PROBLEM_TEMPLATE_1,
                    Map.of("symbiocreationName", sanitizeInput(symbiocreation.getName()),
                            "symbiocreationDescription", sanitizeInput(symbiocreation.getDescription())));
            Message userProblem = userProblemPromptTemplate.createMessage();

            StringBuilder sbUserIdeas = new StringBuilder();
            sbUserIdeas.append("These are some ideas from participants: \n\n");

            existingIdeas.forEach(idea -> {
                String ideaStr = String.format(
                        USER_IDEA_TEMPLATE,
                        Objects.toString(idea.getTitle(), ""),
                        Objects.toString(idea.getDescription(), ""));
                sbUserIdeas.append(ideaStr);
            });
            Message userIdeas = new UserMessage(sbUserIdeas.toString());

            BeanOutputConverter<List<IdeaAiResponse>> outputConverter = new BeanOutputConverter<>(
                    new ParameterizedTypeReference<List<IdeaAiResponse>>() { });
            PromptTemplate userQueryTemplate = new PromptTemplate(
                    USER_QUERY_TEMPLATE_1,
                    Map.of("format", outputConverter.getFormat()));
            Message userQuery = userQueryTemplate.createMessage();

            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(systemForSymbioIdeas), userProblem, userIdeas, userQuery));
            String content = chatClient.prompt(prompt).call().content();
            List<IdeaAiResponse> llmResponse = outputConverter.convert(content);

            return validateResponse(llmResponse);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorResume(e -> {
            log.error("Error getting ideas from LLM for symbiocreation: {}", symbiocreation.getId(), e);
            return Mono.just(Collections.emptyList());
        });
    }

    @Override
    public Mono<List<IdeaAiResponse>> getIdeasForGroupFromLlm(Symbiocreation symbiocreation, Node group) {
        List<Idea> groupIdeas = group.getChildren().stream()
                .map(Node::getIdea)
                .filter(Objects::nonNull)
                .filter(TITLE_OR_DESCRIPTION_EXISTS_PREDICATE)
                .toList();

        if (groupIdeas.isEmpty()) {
            return Mono.just(List.of(new IdeaAiResponse(
                    "Se necesitan más ideas",
                    "Para consolidar ideas del grupo, los participantes deben agregar al menos una idea. " +
                    "Una vez que haya ideas disponibles, la IA podrá generar sugerencias consolidadas."
            )));
        }

        return Mono.fromCallable(() -> {
            PromptTemplate userSessionPromptTemplate = new PromptTemplate(
                    USER_PROBLEM_TEMPLATE_2,
                    Map.of("symbiocreationName", sanitizeInput(symbiocreation.getName()),
                            "symbiocreationDescription", sanitizeInput(symbiocreation.getDescription())));
            Message userSession = userSessionPromptTemplate.createMessage();

            StringBuilder sbUserIdeas = new StringBuilder();
            sbUserIdeas.append("These are the ideas you need to summarize: \n\n");

            groupIdeas.forEach(idea -> {
                String ideaStr = String.format(
                        USER_IDEA_TEMPLATE,
                        Objects.toString(idea.getTitle(), ""),
                        Objects.toString(idea.getDescription(), ""));
                sbUserIdeas.append(ideaStr);
            });
            Message userIdeas = new UserMessage(sbUserIdeas.toString());

            BeanOutputConverter<List<IdeaAiResponse>> outputConverter = new BeanOutputConverter<>(
                    new ParameterizedTypeReference<List<IdeaAiResponse>>() { });
            PromptTemplate userQueryTemplate = new PromptTemplate(
                    USER_QUERY_TEMPLATE_2,
                    Map.of("format", outputConverter.getFormat()));
            Message userQuery = userQueryTemplate.createMessage();

            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(systemForGroupIdeas), userSession, userIdeas, userQuery));
            String content = chatClient.prompt(prompt).call().content();
            List<IdeaAiResponse> llmResponse = outputConverter.convert(content);

            return validateResponse(llmResponse);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorResume(e -> {
            log.error("Error getting ideas from LLM for group: {} in symbiocreation: {}",
                    group.getId(), symbiocreation.getId(), e);
            return Mono.just(Collections.emptyList());
        });
    }

    @Override
    public Mono<Image> getImageFromLlm(IdeaRequest idea) {
        return Mono.fromCallable(() -> {
            String sanitizedTitle = sanitizeInput(idea.title());
            String sanitizedDescription = sanitizeInput(idea.description());
            String messageContent = sanitizedTitle + "\n" + sanitizedDescription;

            ImageMessage imageMessage = new ImageMessage(messageContent);
            ImagePrompt imagePrompt = new ImagePrompt(imageMessage, OpenAiImageOptions.builder()
                    .withQuality("hd")
                    .withN(1)
                    .withHeight(1024)
                    .withWidth(1024)
                    .build());
            return imageModel.call(imagePrompt).getResult().getOutput();
        })
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorResume(e -> {
            log.error("Error generating image from LLM for idea: {}", idea.title(), e);
            return Mono.empty();
        });
    }

    @Override
    public Mono<List<TrendAiResponse>> getTrendsForSymbioFromLlm(Symbiocreation symbiocreation) {
        return Mono.fromCallable(() -> {
            StringBuilder sbIdeas = new StringBuilder();

            List<Idea> ideas = SymbiocreationService.getAllIdeasInSymbiocreation(symbiocreation).stream()
                    .filter(TITLE_OR_DESCRIPTION_EXISTS_PREDICATE)
                    .toList();
            ideas.forEach(idea -> {
                String ideaStr = String.format(
                        USER_IDEA_TEMPLATE,
                        Objects.toString(idea.getTitle(), ""),
                        Objects.toString(idea.getDescription(), ""));
                sbIdeas.append(ideaStr);
            });

            BeanOutputConverter<List<TrendAiResponse>> outputConverter = new BeanOutputConverter<>(
                    new ParameterizedTypeReference<List<TrendAiResponse>>() { });
            PromptTemplate userTrendsPromptTemplate = new PromptTemplate(
                    userForSymbioTrends,
                    Map.of("ideas", sbIdeas,
                            "format", outputConverter.getFormat()));
            Message userTrends = userTrendsPromptTemplate.createMessage();

            Prompt prompt = new Prompt(userTrends);
            String content = chatClient.prompt(prompt).call().content();
            List<TrendAiResponse> llmResponse = outputConverter.convert(content);

            return validateResponse(llmResponse);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorResume(e -> {
            log.error("Error getting trends from LLM for symbiocreation: {}", symbiocreation.getId(), e);
            return Mono.just(Collections.emptyList());
        });
    }

    private List<Idea> getIdeasFromSymbiocreation(Symbiocreation symbiocreation, int maxIdeas) {
        List<Idea> ideas = SymbiocreationService.getAllIdeasInSymbiocreation(symbiocreation).stream()
                .filter(TITLE_OR_DESCRIPTION_EXISTS_PREDICATE)
                .collect(Collectors.toList());
        Collections.shuffle(ideas);
        return ideas.subList(0, Math.min(ideas.size(), maxIdeas));
    }

    private String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }
        // Remove potential prompt injection patterns
        return input
                .replace("ignore", "")
                .replace("Ignore", "")
                .replace("IGNORE", "")
                .replaceAll("(?i)ignore\\s+(all\\s+)?(previous|above|prior)\\s+(instructions?|prompts?)", "")
                .replaceAll("(?i)disregard\\s+(all\\s+)?(previous|above|prior)\\s+(instructions?|prompts?)", "")
                .replaceAll("(?i)forget\\s+(all\\s+)?(previous|above|prior)\\s+(instructions?|prompts?)", "")
                .trim();
    }

    private <T> List<T> validateResponse(List<T> response) {
        if (response == null) {
            return Collections.emptyList();
        }
        return response;
    }
}
