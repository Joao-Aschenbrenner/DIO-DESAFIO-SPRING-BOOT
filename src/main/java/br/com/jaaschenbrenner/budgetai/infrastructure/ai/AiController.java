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
        String prompt = request.text().trim();
        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        if (response == null || response.isBlank()) {
            throw new IllegalStateException("A IA retornou uma resposta vazia.");
        }
        return new AiResponse(response.trim());
    }

    public record AiRequest(
            @NotBlank(message = "O comando não pode ficar vazio.")
            @Size(max = 4000, message = "O comando deve ter no máximo 4000 caracteres.")
            String text) {}

    public record AiResponse(String response) {}
}
