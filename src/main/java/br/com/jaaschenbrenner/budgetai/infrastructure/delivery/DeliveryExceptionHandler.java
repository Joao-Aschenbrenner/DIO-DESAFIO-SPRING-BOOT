package br.com.jaaschenbrenner.budgetai.infrastructure.delivery;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DeliveryExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(DeliveryExceptionHandler.class);

    @ExceptionHandler(TtsNotConfiguredException.class)
    ResponseEntity<ApiError> ttsNotConfigured(TtsNotConfiguredException ex, HttpServletRequest req) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "TTS_NOT_CONFIGURED", ex.getMessage(),
                List.of("fallback=browser-speech"), req, ex, false);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    ResponseEntity<ApiError> missingPart(MissingServletRequestPartException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "AUDIO_REQUIRED", "Selecione um arquivo de áudio WAV ou MP3.",
                List.of(), req, ex, false);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> typeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String message = "Parâmetro inválido: " + ex.getName() + ".";
        return build(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", message, List.of(), req, ex, false);
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ApiError> database(DataAccessException ex, HttpServletRequest req) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "DATABASE_ERROR",
                "Não foi possível acessar o banco local. Feche outras instâncias do Budget AI e tente novamente.",
                List.of(), req, ex, true);
    }

    @ExceptionHandler(ResourceAccessException.class)
    ResponseEntity<ApiError> providerNetwork(ResourceAccessException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_GATEWAY, "PROVIDER_UNREACHABLE",
                "Não foi possível conectar ao serviço de IA. Verifique sua internet e tente novamente.",
                List.of(), req, ex, true);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message, List<String> details,
                                           HttpServletRequest req, Exception ex, boolean logAsError) {
        String correlationId = UUID.randomUUID().toString();
        String path = req == null ? "unknown" : req.getRequestURI();
        if (logAsError) {
            log.error("correlationId={} code={} path={}", correlationId, code, path, ex);
        } else {
            log.warn("correlationId={} code={} path={} message={}", correlationId, code, path, ex.getMessage());
        }
        return ResponseEntity.status(status).body(new ApiError(
                OffsetDateTime.now(), status.value(), code, message, path, correlationId, details));
    }

    public record ApiError(OffsetDateTime timestamp, int status, String code, String message,
                           String path, String correlationId, List<String> details) {}
}
