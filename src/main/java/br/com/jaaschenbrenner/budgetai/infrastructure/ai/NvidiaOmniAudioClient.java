package br.com.jaaschenbrenner.budgetai.infrastructure.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class NvidiaOmniAudioClient {
    private static final long INLINE_LIMIT_BYTES = 180L * 1024L;
    private static final long MAX_AUDIO_BYTES = 50L * 1024L * 1024L;
    private static final String ASSET_DESCRIPTION = "BudgetAI audio";

    private final RestClient inferenceClient;
    private final RestClient assetClient;
    private final HttpClient uploadClient;
    private final String model;

    public NvidiaOmniAudioClient(RestClient.Builder builder,
                                 @Value("${budgetai.ai.base-url}") String baseUrl,
                                 @Value("${spring.ai.openai.chat.api-key}") String apiKey,
                                 @Value("${budgetai.ai.model}") String model) {
        this.model = requireText(model, "Modelo NVIDIA não configurado.");
        String safeBaseUrl = requireText(baseUrl, "Base URL NVIDIA não configurada.");
        String safeApiKey = requireText(apiKey, "NVIDIA API Key não configurada.");

        this.inferenceClient = builder.clone()
                .baseUrl(safeBaseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + safeApiKey)
                .build();
        this.assetClient = builder.clone()
                .baseUrl("https://api.nvcf.nvidia.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + safeApiKey)
                .build();
        this.uploadClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public String transcribe(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Envie um arquivo de áudio WAV ou MP3.");
        }
        if (file.getSize() > MAX_AUDIO_BYTES) {
            throw new IllegalArgumentException("O áudio excede o limite do aplicativo de 50 MB.");
        }

        AudioFormat format = AudioFormat.from(file.getOriginalFilename(), file.getContentType());
        byte[] bytes = file.getBytes();
        if (bytes.length == 0) {
            throw new IllegalArgumentException("O arquivo de áudio está vazio.");
        }

        try {
            String result = bytes.length <= INLINE_LIMIT_BYTES
                    ? transcribeInline(bytes, format)
                    : transcribeUsingAsset(bytes, format);
            if (result == null || result.isBlank()) {
                throw new IllegalStateException("A NVIDIA NIM retornou uma transcrição vazia.");
            }
            return result.trim();
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException(
                    "A NVIDIA NIM recusou a requisição (HTTP " + ex.getStatusCode().value() + ").", ex);
        }
    }

    private String transcribeInline(byte[] bytes, AudioFormat format) {
        String dataUrl = "data:audio/" + format.nvidiaFormat() + ";base64,"
                + Base64.getEncoder().encodeToString(bytes);

        List<Map<String, Object>> content = List.of(
                Map.of("type", "audio_url", "audio_url", Map.of("url", dataUrl)),
                Map.of("type", "text", "text", transcriptionInstruction())
        );

        return invoke(List.of(Map.of("role", "user", "content", content)), null);
    }

    private String transcribeUsingAsset(byte[] bytes, AudioFormat format) throws IOException {
        AssetResponse asset = createAsset(format.uploadContentType());
        if (asset == null || asset.assetId() == null || asset.assetId().isBlank()
                || asset.uploadUrl() == null || asset.uploadUrl().isBlank()) {
            throw new IllegalStateException("A NVIDIA não retornou os dados necessários para o upload temporário do áudio.");
        }

        try {
            uploadAsset(asset.uploadUrl(), bytes, format.uploadContentType());

            String prompt = "<audio src=\"data:audio/" + format.nvidiaFormat()
                    + ";asset_id," + asset.assetId() + "\" />\n" + transcriptionInstruction();

            return invoke(List.of(Map.of("role", "user", "content", prompt)), asset.assetId());
        } finally {
            deleteAssetQuietly(asset.assetId());
        }
    }

    private String transcriptionInstruction() {
        return "Transcribe the speech in the original spoken language. Do not translate. "
                + "Return only the transcription, without comments, summaries or markdown.";
    }

    private String invoke(List<Map<String, Object>> messages, String assetId) {
        ChatRequest payload = new ChatRequest(
                model,
                messages,
                2048,
                false,
                0.2,
                Map.of("enable_thinking", false)
        );

        RestClient.RequestBodySpec request = inferenceClient.post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        if (assetId != null) {
            request.header("NVCF-INPUT-ASSET-REFERENCES", assetId);
        }

        ChatResponse response = request
                .body(payload)
                .retrieve()
                .body(ChatResponse.class);

        if (response == null || response.choices() == null || response.choices().isEmpty()
                || response.choices().getFirst().message() == null
                || response.choices().getFirst().message().content() == null
                || response.choices().getFirst().message().content().isBlank()) {
            throw new IllegalStateException("A NVIDIA NIM retornou uma resposta de áudio vazia.");
        }

        return response.choices().getFirst().message().content().trim();
    }

    private AssetResponse createAsset(String contentType) {
        return assetClient.post()
                .uri("/v2/nvcf/assets")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(new AssetCreateRequest(contentType, ASSET_DESCRIPTION))
                .retrieve()
                .body(AssetResponse.class);
    }

    private void uploadAsset(String uploadUrl, byte[] bytes, String contentType) throws IOException {
        URI uri;
        try {
            uri = URI.create(uploadUrl);
        } catch (IllegalArgumentException ex) {
            throw new IOException("A NVIDIA retornou uma URL de upload inválida.", ex);
        }
        validateUploadUri(uri);

        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofMinutes(5))
                    .header("Content-Type", contentType)
                    .header("x-amz-meta-nvcf-asset-description", ASSET_DESCRIPTION)
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes))
                    .build();

            HttpResponse<Void> response = uploadClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Falha ao enviar o áudio para o armazenamento temporário NVIDIA. HTTP "
                        + response.statusCode());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Upload do áudio para NVIDIA foi interrompido.", ex);
        }
    }

    private void validateUploadUri(URI uri) throws IOException {
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (!"https".equalsIgnoreCase(scheme) || host == null || host.isBlank()) {
            throw new IOException("A NVIDIA retornou uma URL de upload não segura.");
        }

        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if (normalizedHost.equals("localhost")
                || normalizedHost.equals("127.0.0.1")
                || normalizedHost.equals("0.0.0.0")
                || normalizedHost.equals("::1")) {
            throw new IOException("A NVIDIA retornou um destino de upload local inválido.");
        }
    }

    private void deleteAssetQuietly(String assetId) {
        if (assetId == null || assetId.isBlank()) {
            return;
        }
        try {
            assetClient.delete()
                    .uri("/v2/nvcf/assets/{assetId}", assetId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ignored) {
            // Assets temporários também possuem ciclo de vida controlado pela NVIDIA.
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
        return value.trim();
    }

    private record ChatRequest(
            String model,
            List<Map<String, Object>> messages,
            int max_tokens,
            boolean stream,
            double temperature,
            Map<String, Object> chat_template_kwargs) {
    }

    private record ChatResponse(List<Choice> choices) {
    }

    private record Choice(AssistantMessage message) {
    }

    private record AssistantMessage(String content) {
    }

    private record AssetCreateRequest(String contentType, String description) {
    }

    private record AssetResponse(String assetId, String uploadUrl) {
    }

    private record AudioFormat(String nvidiaFormat, String uploadContentType) {
        static AudioFormat from(String filename, String contentType) {
            String name = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
            String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);

            if (name.endsWith(".wav") || type.contains("wav") || type.contains("wave")) {
                return new AudioFormat("wav", "audio/wav");
            }
            if (name.endsWith(".mp3") || type.contains("mpeg") || type.contains("mp3")) {
                return new AudioFormat("mp3", "audio/mpeg");
            }
            throw new IllegalArgumentException(
                    "Formato não suportado pelo NVIDIA Nemotron Omni. Use WAV ou MP3.");
        }
    }
}
