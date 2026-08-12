package br.com.jaaschenbrenner.budgetai.infrastructure.ai;

import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/ai")
public class VoiceCommandController {
    private final TranscriptionModel transcriptionModel;
    private final ChatClient chatClient;

    public VoiceCommandController(TranscriptionModel transcriptionModel, ChatClient chatClient) {
        this.transcriptionModel = transcriptionModel;
        this.chatClient = chatClient;
    }

    @PostMapping(value = "/voice-command", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VoiceCommandResponse voiceCommand(@RequestPart("file") MultipartFile file) throws IOException {
        var resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename() == null ? "audio.mp3" : file.getOriginalFilename();
            }
        };

        String transcription = transcriptionModel.transcribe(resource);
        String response = chatClient.prompt()
                .user(transcription)
                .call()
                .content();

        return new VoiceCommandResponse(transcription, response);
    }

    public record VoiceCommandResponse(String transcription, String response) {}
}
