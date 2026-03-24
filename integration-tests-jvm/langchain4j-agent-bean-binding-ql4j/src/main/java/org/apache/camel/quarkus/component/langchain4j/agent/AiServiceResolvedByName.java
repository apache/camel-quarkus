package org.apache.camel.quarkus.component.langchain4j.agent;

import java.util.function.Supplier;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.apache.camel.Handler;

@ApplicationScoped
@Named("aiServiceResolvedByName")
@RegisterAiService(chatLanguageModelSupplier = AiServiceResolvedByName.AiServiceResolvedByNameModelSupplier.class)
public interface AiServiceResolvedByName {

    public static class AiServiceResolvedByNameModelSupplier implements Supplier<ChatModel> {
        @Override
        public ChatModel get() {
            return new ChatModel() {
                @Override
                public ChatResponse doChat(ChatRequest chatRequest) {
                    return ChatResponse.builder().aiMessage(new AiMessage("AiServiceResolvedByName has been resolved")).build();
                }
            };
        }
    }

    @UserMessage("Any prompt")
    @Handler
    String chatByName(String input);
}
