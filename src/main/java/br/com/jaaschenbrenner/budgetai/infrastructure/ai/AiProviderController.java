package br.com.jaaschenbrenner.budgetai.infrastructure.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class AiProviderController {

    @Value("${budgetai.ai.provider}")
    private String provider;

    @Value("${budgetai.ai.model}")
    private String model;

    @Value("${budgetai.ai.base-url}")
    private String baseUrl;

    @Value("${budgetai.ai.transcription-provider}")
    private String transcriptionProvider;

    @GetMapping("/ai-provider")
    public Map<String, Object> aiProvider() {
        String nvidiaKey = System.getenv("NVIDIA_API_KEY");
        String openAiKey = System.getenv("OPENAI_API_KEY");

        return Map.of(
                "provider", provider,
                "model", model,
                "baseUrl", baseUrl,
                "nvidiaConfigured", nvidiaKey != null && !nvidiaKey.isBlank(),
                "transcriptionProvider", transcriptionProvider,
                "transcriptionConfigured", openAiKey != null && !openAiKey.isBlank(),
                "codexLogin", "separate-cli-auth"
        );
    }
}
