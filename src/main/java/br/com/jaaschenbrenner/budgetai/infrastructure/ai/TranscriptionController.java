package br.com.jaaschenbrenner.budgetai.infrastructure.ai;

import br.com.jaaschenbrenner.budgetai.infrastructure.delivery.AudioUploadValidator;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/ai")
public class TranscriptionController {
    private final NvidiaOmniAudioClient audioClient;

    public TranscriptionController(NvidiaOmniAudioClient audioClient) {
        this.audioClient = audioClient;
    }

    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TranscriptionResponse transcribe(@RequestPart("file") MultipartFile file) throws IOException {
        AudioUploadValidator.validate(file);
        String text = audioClient.transcribe(file);
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("Nenhuma fala reconhecível foi encontrada no áudio.");
        }
        return new TranscriptionResponse(text.trim());
    }

    public record TranscriptionResponse(String transcription) {}
}
