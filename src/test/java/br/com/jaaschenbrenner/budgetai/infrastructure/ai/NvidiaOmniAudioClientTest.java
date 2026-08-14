package br.com.jaaschenbrenner.budgetai.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NvidiaOmniAudioClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void desserializaAssetResponseDaNvidiaEmSnakeCase() throws Exception {
        String nvidiaResponse = """
                {
                  "assetId": "camel-ignored",
                  "uploadUrl": "camel-ignored",
                  "asset_id": "a1b2c3-4d5e",
                  "upload_url": "https://api.nvcf.nvidia.com/v2/nvcf/assets/a1b2c3-4d5e"
                }
                """;

        NvidiaOmniAudioClient.AssetResponse parsed =
                objectMapper.readValue(nvidiaResponse, NvidiaOmniAudioClient.AssetResponse.class);

        assertThat(parsed.assetId()).isEqualTo("a1b2c3-4d5e");
        assertThat(parsed.uploadUrl()).isEqualTo("https://api.nvcf.nvidia.com/v2/nvcf/assets/a1b2c3-4d5e");
    }

    @Test
    void fluxoInlinePreservaDataUrlSemAssetTemporario() {
        NvidiaOmniAudioClient client = new NvidiaOmniAudioClient(
                RestClient.builder(), "http://localhost:9999", "test-key", "nvidia/test-model");

        byte[] audio = new byte[] {0, 1, 2, 3, 4, 5};
        NvidiaOmniAudioClient.AudioFormat format =
                NvidiaOmniAudioClient.AudioFormat.from("gravacao.wav", "audio/wav");

        List<Map<String, Object>> content = client.inlineContent(audio, format);

        assertThat(content).hasSize(2);

        @SuppressWarnings("unchecked")
        Map<String, Object> audioUrl = (Map<String, Object>) content.getFirst().get("audio_url");
        String url = (String) audioUrl.get("url");

        assertThat(url).startsWith("data:audio/wav;base64,");
        String base64 = url.substring("data:audio/wav;base64,".length());
        assertThat(Base64.getDecoder().decode(base64)).containsExactly(audio);
    }
}
