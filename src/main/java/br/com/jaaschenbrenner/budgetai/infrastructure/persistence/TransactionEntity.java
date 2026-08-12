package br.com.jaaschenbrenner.budgetai.infrastructure.persistence;

import br.com.jaaschenbrenner.budgetai.domain.Category;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class TransactionEntity {
    @Id
    private UUID id;
    private String description;
    private long amountInCents;
    @Enumerated(EnumType.STRING)
    private Category category;
    private LocalDateTime createdAt;

    protected TransactionEntity() {}

    public TransactionEntity(UUID id, String description, long amountInCents, Category category, LocalDateTime createdAt) {
        this.id = id;
        this.description = description;
        this.amountInCents = amountInCents;
        this.category = category;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getDescription() { return description; }
    public long getAmountInCents() { return amountInCents; }
    public Category getCategory() { return category; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
