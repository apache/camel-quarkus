package org.apache.camel.quarkus.component.langchain4j.agent;

import java.util.function.Supplier;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.Handler;

@ApplicationScoped
@RegisterAiService(chatLanguageModelSupplier = AiServiceResolvedByInterface.AiServiceResolvedByInterfaceModelSupplier.class)
public interface AiServiceResolvedByInterface {

    public static class AiServiceResolvedByInterfaceModelSupplier implements Supplier<ChatModel> {
        @Override
        public ChatModel get() {
            return new ChatModel() {
                @Override
                public ChatResponse doChat(ChatRequest chatRequest) {
                    return ChatResponse.builder().aiMessage(new AiMessage("AiServiceResolvedByInterface has been resolved"))
                            .build();
                }
            };
        }
    }

    @UserMessage("Any prompt")
    @Handler
    String chat(String input);
}
