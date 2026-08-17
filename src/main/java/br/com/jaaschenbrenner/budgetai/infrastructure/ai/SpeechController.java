package br.com.jaaschenbrenner.budgetai.infrastructure.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/ai/speech")
public class SpeechController {
    private final ObjectProvider<TextToSpeechModel> speechModelProvider;
    private final String configuredProvider;

    public SpeechController(
            ObjectProvider<TextToSpeechModel> speechModelProvider,
            @Value("${spring.ai.model.audio.speech:none}") String configuredProvider) {
        this.speechModelProvider = speechModelProvider;
        this.configuredProvider = configuredProvider;
    }

    @GetMapping("/status")
    public SpeechStatus status() {
        TextToSpeechModel model = speechModelProvider.getIfAvailable();
        boolean available = model != null;
        return new SpeechStatus(
                available,
                available ? configuredProvider : "none",
                available
                        ? "Spring AI TextToSpeechModel disponível para gerar MP3."
                        : "TTS cloud desativado; a interface usará síntese de voz local quando disponível.");
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = "audio/mpeg")
    public ResponseEntity<byte[]> synthesize(@Valid @RequestBody SpeechRequest request) {
        TextToSpeechModel model = speechModelProvider.getIfAvailable();
        if (model == null) {
            throw new SpeechOutputUnavailableException(
                    "O TTS Spring AI não está configurado. Use o fallback local da interface ou informe uma chave de TTS no launcher.");
        }

        byte[] audio = model.call(request.text().trim());
        if (audio == null || audio.length == 0) {
            throw new IllegalStateException("O provedor de TTS retornou um áudio vazio.");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"budget-ai-response.mp3\"")
                .body(audio);
    }

    public record SpeechRequest(
            @NotBlank(message = "Informe um texto para gerar a resposta falada.")
            @Size(max = 4000, message = "O texto para TTS deve ter no máximo 4000 caracteres.")
            String text) {}

    public record SpeechStatus(boolean available, String provider, String message) {}
}
