package br.com.jaaschenbrenner.budgetai.infrastructure.delivery;

import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

public final class AudioUploadValidator {
    private static final long MAX_BYTES = 50L * 1024L * 1024L;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "audio/wav", "audio/x-wav", "audio/wave", "audio/vnd.wave",
            "audio/mpeg", "audio/mp3", "application/octet-stream");

    private AudioUploadValidator() {}

    public static void validate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new IllegalArgumentException("Selecione um arquivo de áudio WAV ou MP3.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("O áudio deve ter no máximo 50 MB.");
        }

        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        boolean extensionAllowed = filename.endsWith(".wav") || filename.endsWith(".mp3");

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        boolean typeAllowed = ALLOWED_TYPES.contains(contentType);

        if (!extensionAllowed || !typeAllowed) {
            throw new IllegalArgumentException("Formato de áudio não suportado. Envie um arquivo WAV ou MP3.");
        }
    }
}
