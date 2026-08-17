package br.com.jaaschenbrenner.budgetai.infrastructure.web;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class UiDeliveryContractTest {

    @Test
    void deliveryUiMustExplainChallengeAndReflowOnSmallScreens() throws Exception {
        String html = resource("/static/index.html");
        String css = resource("/static/app.css");

        assertThat(html).contains("width=device-width");
        assertThat(html).contains("Fluxo principal do desafio");
        assertThat(html).contains("Experimente o fluxo completo por voz");
        assertThat(html).contains("Tool Calling");
        assertThat(html).contains("Valor (R$)");
        assertThat(html).contains("/app.css");
        assertThat(html).contains("/app.js");
        assertThat(html).doesNotContain("<pre id=\"transactions\"");

        assertThat(css).contains("@media (max-width:900px)");
        assertThat(css).contains("@media (max-width:700px)");
        assertThat(css).contains("@media (max-width:480px)");
        assertThat(css).contains("minmax(0,1fr)");
        assertThat(css).doesNotContain("minmax(300px");
    }

    private String resource(String path) throws Exception {
        try (InputStream input = getClass().getResourceAsStream(path)) {
            assertThat(input).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
