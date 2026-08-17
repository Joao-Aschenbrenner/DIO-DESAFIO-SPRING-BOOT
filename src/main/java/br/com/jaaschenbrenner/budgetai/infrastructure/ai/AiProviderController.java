package br.com.jaaschenbrenner.budgetai.infrastructure.ai;

import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class AiProviderController {

    private final ObjectProvider<TextToSpeechModel> speechModelProvider;

    @Value("${budgetai.ai.provider}")
    private String provider;

    @Value("${budgetai.ai.model}")
    private String model;

    @Value("${budgetai.ai.base-url}")
    private String baseUrl;

    @Value("${budgetai.ai.transcription-provider}")
    private String transcriptionProvider;

    @Value("${spring.ai.openai.chat.api-key:not-configured}")
    private String nvidiaApiKey;

    @Value("${spring.ai.model.audio.speech:none}")
    private String speechProvider;

    public AiProviderController(ObjectProvider<TextToSpeechModel> speechModelProvider) {
        this.speechModelProvider = speechModelProvider;
    }

    @GetMapping("/ai-provider")
    public Map<String, Object> aiProvider() {
        boolean configured = isConfiguredSecret(nvidiaApiKey);
        boolean springAiSpeech = speechModelProvider.getIfAvailable() != null;

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("provider", provider);
        status.put("model", model);
        status.put("baseUrl", baseUrl);
        status.put("nvidiaConfigured", configured);
        status.put("textConfigured", configured);
        status.put("audioConfigured", configured);
        status.put("transcriptionProvider", transcriptionProvider);
        status.put("modalities", "text,audio");
        status.put("toolCalling", true);
        status.put("springAiSpeechAvailable", springAiSpeech);
        status.put("speechProvider", springAiSpeech ? speechProvider : "none");
        status.put("speechFallback", "Web Speech API local (pt-BR)");
        status.put("localOnlyServer", true);
        return status;
    }

    private boolean isConfiguredSecret(String value) {
        return value != null
                && !value.isBlank()
                && !"not-configured".equalsIgnoreCase(value)
                && !value.startsWith("${");
    }
}
