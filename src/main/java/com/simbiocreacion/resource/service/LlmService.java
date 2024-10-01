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
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
        PromptTemplate userProblemPromptTemplate = new PromptTemplate(
                USER_PROBLEM_TEMPLATE_1,
                Map.of("symbiocreationName", symbiocreation.getName(),
                        "symbiocreationDescription", Objects.isNull(symbiocreation.getDescription())
                                ? "" : symbiocreation.getDescription()));
        Message userProblem = userProblemPromptTemplate.createMessage();

        StringBuilder sbUserIdeas = new StringBuilder();
        sbUserIdeas.append("These are some ideas from participants: \n\n");

        List<Idea> ideas = getThreeIdeasFromSymbiocreation(symbiocreation);
        ideas.forEach(idea -> {
            String ideaStr = String.format(
                    USER_IDEA_TEMPLATE,
                    idea.getTitle(),
                    idea.getDescription());
            sbUserIdeas.append(ideaStr);
        });
        Message userIdeas = new UserMessage(sbUserIdeas.toString());

        BeanOutputConverter<List<IdeaAiResponse>> outputConverter = new BeanOutputConverter<>(
                new ParameterizedTypeReference<List<IdeaAiResponse>>() { });
        PromptTemplate userQueryTemplate = new PromptTemplate(
                USER_QUERY_TEMPLATE_1,
                Map.of("format", outputConverter.getFormat())); // prompt is for role 'user' by default
        Message userQuery = userQueryTemplate.createMessage();

        Prompt prompt = new Prompt(List.of(
                new SystemMessage(systemForSymbioIdeas), userProblem, userIdeas, userQuery));
        List<IdeaAiResponse> llmResponse = outputConverter.convert(chatClient.prompt(prompt).call().content());

        return Mono.just(llmResponse);
    }

    @Override
    public Mono<List<IdeaAiResponse>> getIdeasForGroupFromLlm(Symbiocreation symbiocreation, Node group) {
        PromptTemplate userSessionPromptTemplate = new PromptTemplate(
                USER_PROBLEM_TEMPLATE_2,
                Map.of("symbiocreationName", symbiocreation.getName(),
                        "symbiocreationDescription", Objects.isNull(symbiocreation.getDescription())
                                ? "" : symbiocreation.getDescription()));
        Message userSession = userSessionPromptTemplate.createMessage();

        StringBuilder sbUserIdeas = new StringBuilder();
        sbUserIdeas.append("These are the ideas you need to summarize: \n\n");

        List<Idea> ideas = group.getChildren().stream()
                .map(Node::getIdea)
                .filter(Objects::nonNull)
                .filter(TITLE_OR_DESCRIPTION_EXISTS_PREDICATE)
                .toList();
        ideas.forEach(idea -> {
            String ideaStr = String.format(
                    USER_IDEA_TEMPLATE,
                    idea.getTitle(),
                    idea.getDescription());
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
        List<IdeaAiResponse> llmResponse = outputConverter.convert(chatClient.prompt(prompt).call().content());

        return Mono.just(llmResponse);
    }

    @Override
    public Mono<Image> getImageFromLlm(IdeaRequest idea) {
        String messageContent = idea.title() + "\n" + idea.description();

        ImageMessage imageMessage = new ImageMessage(messageContent);
        ImagePrompt imagePrompt = new ImagePrompt(imageMessage, OpenAiImageOptions.builder()
                .withQuality("hd")
                .withN(1)
                .withHeight(1024)
                .withWidth(1024)
                .build());
        Image image = imageModel.call(imagePrompt).getResult().getOutput();

        return Mono.just(image);
    }

    @Override
    public Mono<List<TrendAiResponse>> getTrendsForSymbioFromLlm(Symbiocreation symbiocreation) {
        StringBuilder sbIdeas = new StringBuilder();

        List<Idea> ideas = SymbiocreationService.getAllIdeasInSymbiocreation(symbiocreation).stream()
                .filter(TITLE_OR_DESCRIPTION_EXISTS_PREDICATE)
                .toList();
        ideas.forEach(idea -> {
            String ideaStr = String.format(
                    USER_IDEA_TEMPLATE,
                    idea.getTitle(),
                    idea.getDescription());
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
        List<TrendAiResponse> llmResponse = outputConverter.convert(chatClient.prompt(prompt).call().content());

        return Mono.just(llmResponse);
    }

    private List<Idea> getThreeIdeasFromSymbiocreation(Symbiocreation symbiocreation) {
        List<Idea> ideas = SymbiocreationService.getAllIdeasInSymbiocreation(symbiocreation).stream()
                .filter(TITLE_OR_DESCRIPTION_EXISTS_PREDICATE)
                .collect(Collectors.toList());
        Collections.shuffle(ideas); // random shuffle
        return ideas.subList(0, 3);
    }
}
