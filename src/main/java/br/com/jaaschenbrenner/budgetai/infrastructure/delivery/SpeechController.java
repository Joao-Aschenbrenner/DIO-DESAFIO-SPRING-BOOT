package br.com.jaaschenbrenner.budgetai.infrastructure.delivery;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class SpeechController {
    private static final MediaType AUDIO_MPEG = MediaType.parseMediaType("audio/mpeg");

    private final ObjectProvider<TextToSpeechModel> speechModels;

    public SpeechController(ObjectProvider<TextToSpeechModel> speechModels) {
        this.speechModels = speechModels;
    }

    @PostMapping(value = "/speech", produces = "audio/mpeg")
    public ResponseEntity<byte[]> speech(@Valid @RequestBody SpeechRequest request) {
        if (!isTtsKeyConfigured()) {
            throw new TtsNotConfiguredException(
                    "A voz MP3 do Spring AI é opcional e ainda não possui uma BUDGETAI_TTS_API_KEY configurada.");
        }

        TextToSpeechModel model = speechModels.getIfAvailable();
        if (model == null) {
            throw new TtsNotConfiguredException("O TextToSpeechModel do Spring AI não está disponível nesta execução.");
        }

        byte[] audio = model.call(request.text().trim());
        if (audio == null || audio.length < 128) {
            throw new IllegalStateException("O provedor de voz retornou um áudio vazio ou inválido.");
        }

        return ResponseEntity.ok()
                .contentType(AUDIO_MPEG)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-BudgetAI-Speech-Engine", "Spring-AI-TextToSpeechModel")
                .body(audio);
    }

    public static boolean isTtsKeyConfigured() {
        String key = System.getenv("BUDGETAI_TTS_API_KEY");
        return key != null && !key.isBlank();
    }

    public record SpeechRequest(
            @NotBlank(message = "O texto para voz não pode ficar vazio.")
            @Size(max = 2000, message = "O texto para voz deve ter no máximo 2000 caracteres.")
            String text) {}
}
