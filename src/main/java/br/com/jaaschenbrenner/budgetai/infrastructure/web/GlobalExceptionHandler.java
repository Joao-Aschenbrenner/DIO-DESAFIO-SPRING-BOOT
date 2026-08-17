package br.com.jaaschenbrenner.budgetai.infrastructure.web;

import br.com.jaaschenbrenner.budgetai.infrastructure.ai.SpeechOutputUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Revise os dados informados.", details, req, ex, false);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                "Não foi possível interpretar os dados enviados. Verifique os campos e tente novamente.",
                List.of(), req, ex, false);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> typeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "Um parâmetro possui valor inválido.",
                List.of(ex.getName() + ": " + String.valueOf(ex.getValue())), req, ex, false);
    }

    @ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class})
    ResponseEntity<ApiError> missingInput(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "MISSING_INPUT",
                "Faltou uma informação obrigatória na requisição.", List.of(), req, ex, false);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> badRequest(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message(ex, "Requisição inválida."), List.of(), req, ex, false);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> tooLarge(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
                "O arquivo excede o limite de 50 MB.", List.of(), req, ex, false);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiError> mediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                "O formato enviado não é suportado neste endpoint.", List.of(), req, ex, false);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> methodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "Este método HTTP não é aceito neste endpoint.", List.of(), req, ex, false);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> notFound(NoResourceFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "Recurso não encontrado.", List.of(), req, ex, false);
    }

    @ExceptionHandler(SpeechOutputUnavailableException.class)
    ResponseEntity<ApiError> speechUnavailable(SpeechOutputUnavailableException ex, HttpServletRequest req) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "TTS_UNAVAILABLE",
                message(ex, "O TTS Spring AI não está disponível."), List.of(), req, ex, false);
    }

    @ExceptionHandler(RestClientResponseException.class)
    ResponseEntity<ApiError> provider(RestClientResponseException ex, HttpServletRequest req) {
        int status = ex.getStatusCode().value();
        boolean tts = requestPath(req).startsWith("/api/ai/speech");
        String provider = tts ? "provedor de voz" : "NVIDIA";
        String msg = switch (status) {
            case 401, 403 -> tts
                    ? "O provedor de voz recusou a credencial configurada."
                    : "A NVIDIA recusou a credencial. Verifique sua NVIDIA API Key.";
            case 404 -> "Modelo ou endpoint do " + provider + " não foi encontrado.";
            case 408 -> "A solicitação ao " + provider + " excedeu o tempo esperado.";
            case 429 -> "O limite de requisições do " + provider + " foi atingido. Aguarde e tente novamente.";
            default -> status >= 500
                    ? "O " + provider + " está temporariamente indisponível."
                    : "O " + provider + " recusou a requisição.";
        };
        return build(HttpStatus.BAD_GATEWAY, tts ? "TTS_PROVIDER_ERROR" : "AI_PROVIDER_ERROR",
                msg, List.of("providerStatus=" + status), req, ex, true);
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ApiError> dataAccess(DataAccessException ex, HttpServletRequest req) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "PERSISTENCE_ERROR",
                "Não foi possível acessar os dados locais agora. Feche outras instâncias do Budget AI e tente novamente.",
                List.of(), req, ex, true);
    }

    @ExceptionHandler(IOException.class)
    ResponseEntity<ApiError> io(IOException ex, HttpServletRequest req) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "FILE_PROCESSING_ERROR",
                "Não foi possível processar o arquivo enviado.", List.of(), req, ex, true);
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ApiError> state(IllegalStateException ex, HttpServletRequest req) {
        String normalized = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase(Locale.ROOT);
        boolean nvidia = normalized.contains("nvidia");
        boolean speech = normalized.contains("tts") || normalized.contains("voz") || requestPath(req).startsWith("/api/ai/speech");

        if (nvidia) {
            return build(HttpStatus.BAD_GATEWAY, "AI_PROVIDER_ERROR",
                    "Não foi possível concluir a operação com a NVIDIA. Confira a credencial, conexão e tente novamente.",
                    List.of(), req, ex, true);
        }
        if (speech) {
            return build(HttpStatus.BAD_GATEWAY, "TTS_PROVIDER_ERROR",
                    "Não foi possível gerar o áudio remoto. A resposta em texto continua disponível e a interface pode usar voz local.",
                    List.of(), req, ex, true);
        }
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "APPLICATION_STATE_ERROR",
                "A aplicação encontrou um estado inesperado.", List.of(), req, ex, true);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "UNEXPECTED_ERROR",
                "Ocorreu um erro inesperado. Use o código de correlação caso precise consultar o log.",
                List.of(), req, ex, true);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String msg, List<String> details,
                                           HttpServletRequest req, Exception ex, boolean error) {
        String correlationId = UUID.randomUUID().toString();
        String path = requestPath(req);
        if (error) {
            log.error("correlationId={} code={} path={}", correlationId, code, path, ex);
        } else {
            log.warn("correlationId={} code={} path={} message={}", correlationId, code, path, ex.getMessage());
        }
        return ResponseEntity.status(status)
                .body(new ApiError(OffsetDateTime.now(), status.value(), code, msg, path, correlationId, details));
    }

    private String requestPath(HttpServletRequest req) {
        return req == null || req.getRequestURI() == null ? "unknown" : req.getRequestURI();
    }

    private String message(Exception ex, String fallback) {
        return ex.getMessage() == null || ex.getMessage().isBlank() ? fallback : ex.getMessage();
    }

    public record ApiError(OffsetDateTime timestamp, int status, String code, String message,
                           String path, String correlationId, List<String> details) {}
}
