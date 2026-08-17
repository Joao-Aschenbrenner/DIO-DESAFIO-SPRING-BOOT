package br.com.jaaschenbrenner.budgetai.infrastructure.delivery;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class AudioUploadValidatorTest {

    @Test
    void acceptsWavAndMp3() {
        assertThatCode(() -> AudioUploadValidator.validate(new MockMultipartFile(
                "file", "teste.wav", "audio/wav", new byte[]{1, 2, 3}))).doesNotThrowAnyException();
        assertThatCode(() -> AudioUploadValidator.validate(new MockMultipartFile(
                "file", "teste.mp3", "audio/mpeg", new byte[]{1, 2, 3}))).doesNotThrowAnyException();
    }

    @Test
    void rejectsEmptyAndUnsupportedFiles() {
        assertThatThrownBy(() -> AudioUploadValidator.validate(new MockMultipartFile(
                "file", "vazio.wav", "audio/wav", new byte[0])))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Selecione");

        assertThatThrownBy(() -> AudioUploadValidator.validate(new MockMultipartFile(
                "file", "arquivo.txt", "text/plain", new byte[]{1})))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("WAV ou MP3");
    }
}
