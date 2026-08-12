package br.com.jaaschenbrenner.budgetai.domain;

import java.util.UUID;

public record TransactionId(UUID value) {
    public TransactionId {
        if (value == null) throw new IllegalArgumentException("TransactionId não pode ser nulo");
    }

    public static TransactionId newId() {
        return new TransactionId(UUID.randomUUID());
    }
}
