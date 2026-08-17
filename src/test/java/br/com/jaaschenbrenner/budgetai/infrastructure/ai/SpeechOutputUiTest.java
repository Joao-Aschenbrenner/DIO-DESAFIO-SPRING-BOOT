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
    void shouldPreferSpringAiSpeechAndKeepLocalFallback() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/static/app.js")) {
            assertNotNull(input, "app.js must be available on the classpath");
            String js = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            assertAll(
                    () -> assertTrue(js.contains("/api/ai/speech"), "Spring AI speech endpoint must be used"),
                    () -> assertTrue(js.contains("remoteSpeak"), "remote Spring AI speech path must exist"),
                    () -> assertTrue(js.contains("speechSynthesis"), "Web Speech fallback must be wired"),
                    () -> assertTrue(js.contains("SpeechSynthesisUtterance"), "local TTS utterance must be created"),
                    () -> assertTrue(js.contains("autoSpeak"), "automatic spoken response toggle must exist"),
                    () -> assertTrue(js.contains("pt-BR"), "local fallback should prefer Brazilian Portuguese")
            );
        }
    }
}
