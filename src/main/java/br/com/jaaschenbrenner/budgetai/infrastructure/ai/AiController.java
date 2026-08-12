package br.com.jaaschenbrenner.budgetai.infrastructure.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final ChatClient chatClient;

    public AiController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping("/command")
    public AiResponse command(@RequestBody AiRequest request) {
        String response = chatClient.prompt()
                .user(request.text())
                .call()
                .content();
        return new AiResponse(response);
    }

    public record AiRequest(String text) {}
    public record AiResponse(String response) {}
}
