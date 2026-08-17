package br.com.jaaschenbrenner.budgetai.infrastructure.ai;

import br.com.jaaschenbrenner.budgetai.infrastructure.delivery.SpeechController;
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

    private final ObjectProvider<TextToSpeechModel> speechModels;

    @Value("${budgetai.ai.provider}")
    private String provider;

    @Value("${budgetai.ai.model}")
    private String model;

    @Value("${budgetai.ai.base-url}")
    private String baseUrl;

    @Value("${budgetai.ai.transcription-provider}")
    private String transcriptionProvider;

    public AiProviderController(ObjectProvider<TextToSpeechModel> speechModels) {
        this.speechModels = speechModels;
    }

    @GetMapping("/ai-provider")
    public Map<String, Object> aiProvider() {
        String nvidiaKey = System.getenv("NVIDIA_API_KEY");
        boolean nvidiaConfigured = nvidiaKey != null && !nvidiaKey.isBlank();
        boolean springTtsConfigured = SpeechController.isTtsKeyConfigured() && speechModels.getIfAvailable() != null;

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("provider", provider);
        status.put("model", model);
        status.put("baseUrl", baseUrl);
        status.put("nvidiaConfigured", nvidiaConfigured);
        status.put("textConfigured", nvidiaConfigured);
        status.put("audioInputConfigured", nvidiaConfigured);
        status.put("transcriptionProvider", transcriptionProvider);
        status.put("modalities", "text,audio-input");
        status.put("toolCalling", true);
        status.put("springTtsAvailable", springTtsConfigured);
        status.put("speechOutput", springTtsConfigured
                ? "Spring AI TextToSpeechModel / MP3"
                : "Web Speech API local (fallback)");
        status.put("healthy", true);
        return status;
    }
}
