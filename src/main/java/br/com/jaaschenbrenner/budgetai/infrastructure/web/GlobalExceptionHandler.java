package br.com.jaaschenbrenner.budgetai.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage()).toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Dados inválidos.", details, req, ex, false);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                "Não foi possível interpretar os dados enviados. Verifique a categoria e os demais campos.",
                List.of(), req, ex, false);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> badRequest(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message(ex, "Requisição inválida."), List.of(), req, ex, false);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> tooLarge(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "O arquivo excede o limite de 50 MB.", List.of(), req, ex, false);
    }

    @ExceptionHandler(RestClientResponseException.class)
    ResponseEntity<ApiError> provider(RestClientResponseException ex, HttpServletRequest req) {
        int s = ex.getStatusCode().value();
        String msg = switch (s) {
            case 401, 403 -> "A NVIDIA recusou a credencial. Verifique sua NVIDIA API Key.";
            case 404 -> "Modelo ou endpoint NVIDIA não encontrado.";
            case 429 -> "Limite de requisições NVIDIA atingido. Aguarde e tente novamente.";
            default -> s >= 500 ? "O serviço NVIDIA está temporariamente indisponível." : "A NVIDIA recusou a requisição.";
        };
        return build(HttpStatus.BAD_GATEWAY, "AI_PROVIDER_ERROR", msg, List.of("providerStatus=" + s), req, ex, true);
    }

    @ExceptionHandler(IOException.class)
    ResponseEntity<ApiError> io(IOException ex, HttpServletRequest req) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "FILE_PROCESSING_ERROR", "Não foi possível processar o arquivo enviado.", List.of(), req, ex, true);
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ApiError> state(IllegalStateException ex, HttpServletRequest req) {
        boolean nvidia = ex.getMessage() != null && ex.getMessage().toLowerCase().contains("nvidia");
        return build(nvidia ? HttpStatus.BAD_GATEWAY : HttpStatus.INTERNAL_SERVER_ERROR,
                nvidia ? "AI_PROVIDER_ERROR" : "APPLICATION_STATE_ERROR",
                nvidia ? message(ex, "Falha no provedor NVIDIA.") : "A aplicação encontrou um estado inesperado.",
                List.of(), req, ex, true);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "UNEXPECTED_ERROR",
                "Erro inesperado. Informe o código de correlação ao consultar o log.", List.of(), req, ex, true);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String msg, List<String> details,
                                           HttpServletRequest req, Exception ex, boolean error) {
        String correlationId = UUID.randomUUID().toString();
        String path = req == null ? "unknown" : req.getRequestURI();
        if (error) log.error("correlationId={} code={} path={}", correlationId, code, path, ex);
        else log.warn("correlationId={} code={} path={} message={}", correlationId, code, path, ex.getMessage());
        return ResponseEntity.status(status).body(new ApiError(OffsetDateTime.now(), status.value(), code, msg, path, correlationId, details));
    }

    private String message(Exception ex, String fallback) {
        return ex.getMessage() == null || ex.getMessage().isBlank() ? fallback : ex.getMessage();
    }

    public record ApiError(OffsetDateTime timestamp, int status, String code, String message,
                           String path, String correlationId, List<String> details) {}
}
