package br.com.jaaschenbrenner.budgetai.infrastructure.web;

import br.com.jaaschenbrenner.budgetai.infrastructure.ai.SpeechOutputUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerContractTest {

    @Test
    void businessValidationMustReturn400WithCorrelationId() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest request = request("/api/transactions");

        var response = handler.badRequest(new IllegalArgumentException("valor inválido"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_REQUEST");
        assertThat(response.getBody().correlationId()).isNotBlank();
    }

    @Test
    void optionalTtsMissingMustNotBecomeGeneric500() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest request = request("/api/ai/speech");

        var response = handler.speechUnavailable(
                new SpeechOutputUnavailableException("TTS opcional não configurado"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("TTS_UNAVAILABLE");
    }

    private HttpServletRequest request(String path) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(path);
        return request;
    }
}
