package br.com.jaaschenbrenner.budgetai.infrastructure.ai;

public class SpeechOutputUnavailableException extends RuntimeException {
    public SpeechOutputUnavailableException(String message) {
        super(message);
    }
}
