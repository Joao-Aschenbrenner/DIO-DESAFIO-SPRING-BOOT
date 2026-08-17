package br.com.jaaschenbrenner.budgetai.infrastructure.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    public AiResponse command(@Valid @RequestBody AiRequest request) {
        String response = chatClient.prompt()
                .user(request.text().trim())
                .call()
                .content();

        if (response == null || response.isBlank()) {
            throw new IllegalStateException("O modelo de IA retornou uma resposta vazia.");
        }
        return new AiResponse(response.trim());
    }

    public record AiRequest(
            @NotBlank(message = "Informe um comando para a IA.")
            @Size(max = 1200, message = "O comando deve ter no máximo 1200 caracteres.")
            String text) {}

    public record AiResponse(String response) {}
}
