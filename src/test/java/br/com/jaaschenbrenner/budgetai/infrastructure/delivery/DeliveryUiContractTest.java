package br.com.jaaschenbrenner.budgetai.infrastructure.delivery;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryUiContractTest {

    @Test
    void deliveryUiMustExplainChallengeAndRemainMobileFriendly() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/static/app.html")) {
            assertThat(input).isNotNull();
            String html = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(html).contains("Experimente o fluxo do desafio");
            assertThat(html).contains("Transforme um comando de voz em uma transação financeira real");
            assertThat(html).contains("Tool Calling");
            assertThat(html).contains("Valor (R$)");
            assertThat(html).contains("/api/ai/transcribe");
            assertThat(html).contains("/api/ai/command");
            assertThat(html).contains("/api/ai/speech");
            assertThat(html).contains("Spring AI TextToSpeechModel");
            assertThat(html).contains("@media (max-width:360px)");
            assertThat(html).doesNotContain("Valor em centavos");
            assertThat(html).doesNotContain("minmax(300px");
            assertThat(html).doesNotContain("<option>TRANSPORT</option>");
        }
    }
}
