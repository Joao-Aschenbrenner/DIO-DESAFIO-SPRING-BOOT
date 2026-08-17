package br.com.jaaschenbrenner.budgetai;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationPropertiesHardeningTest {

    @Test
    void desktopServerMustStayLocalAndOptionalFeaturesOffByDefault() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/application.properties")) {
            assertThat(input).isNotNull();
            String properties = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(properties)
                    .contains("server.address=127.0.0.1")
                    .contains("spring.h2.console.enabled=${BUDGETAI_H2_CONSOLE:false}")
                    .contains("spring.ai.model.audio.speech=${BUDGETAI_TTS_PROVIDER:none}");
        }
    }
}
