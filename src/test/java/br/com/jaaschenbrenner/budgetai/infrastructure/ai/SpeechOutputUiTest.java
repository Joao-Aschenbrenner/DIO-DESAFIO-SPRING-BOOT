package br.com.jaaschenbrenner.budgetai.infrastructure.ai;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpeechOutputUiTest {

    @Test
    void shouldExposeLocalTextToSpeechForAiResponses() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/static/index.html")) {
            assertNotNull(input, "index.html must be available on the classpath");
            String html = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            assertAll(
                    () -> assertTrue(html.contains("speechSynthesis"), "Web Speech API must be wired"),
                    () -> assertTrue(html.contains("SpeechSynthesisUtterance"), "TTS utterance must be created"),
                    () -> assertTrue(html.contains("lastSpeech.audio=x.response"), "voice-command response must feed TTS"),
                    () -> assertTrue(html.contains("autoSpeak"), "automatic spoken response toggle must exist"),
                    () -> assertTrue(html.contains("pt-BR"), "spoken response should prefer Brazilian Portuguese")
            );
        }
    }
}
