package br.com.jaaschenbrenner.budgetai.infrastructure.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpeechControllerTest {

    @Test
    void reportsLocalFallbackWhenCloudTtsIsDisabled() {
        @SuppressWarnings("unchecked")
        ObjectProvider<TextToSpeechModel> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);

        SpeechController controller = new SpeechController(provider, "none");

        assertThat(controller.status().available()).isFalse();
        assertThatThrownBy(() -> controller.synthesize(new SpeechController.SpeechRequest("Olá")))
                .isInstanceOf(SpeechOutputUnavailableException.class);
    }

    @Test
    void returnsMp3WhenSpringAiTtsIsAvailable() {
        @SuppressWarnings("unchecked")
        ObjectProvider<TextToSpeechModel> provider = mock(ObjectProvider.class);
        TextToSpeechModel model = mock(TextToSpeechModel.class);
        when(provider.getIfAvailable()).thenReturn(model);
        when(model.call("Olá")).thenReturn(new byte[]{1, 2, 3});

        SpeechController controller = new SpeechController(provider, "openai");
        var response = controller.synthesize(new SpeechController.SpeechRequest("Olá"));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType("audio/mpeg"));
        assertThat(response.getBody()).containsExactly((byte) 1, (byte) 2, (byte) 3);
    }
}
