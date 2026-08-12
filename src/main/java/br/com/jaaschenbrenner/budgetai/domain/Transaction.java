package br.com.jaaschenbrenner.budgetai.domain;

import java.time.LocalDateTime;

public class Transaction {
    private final TransactionId id;
    private final String description;
    private final long amountInCents;
    private final Category category;
    private final LocalDateTime createdAt;

    public Transaction(TransactionId id, String description, long amountInCents, Category category, LocalDateTime createdAt) {
        if (id == null) throw new IllegalArgumentException("id é obrigatório");
        if (description == null || description.isBlank()) throw new IllegalArgumentException("description é obrigatória");
        if (amountInCents <= 0) throw new IllegalArgumentException("amountInCents deve ser maior que zero");
        if (category == null) throw new IllegalArgumentException("category é obrigatória");
        this.id = id;
        this.description = description.trim();
        this.amountInCents = amountInCents;
        this.category = category;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }

    public static Transaction create(String description, long amountInCents, Category category) {
        return new Transaction(TransactionId.newId(), description, amountInCents, category, LocalDateTime.now());
    }

    public TransactionId id() { return id; }
    public String description() { return description; }
    public long amountInCents() { return amountInCents; }
    public Category category() { return category; }
    public LocalDateTime createdAt() { return createdAt; }
}
