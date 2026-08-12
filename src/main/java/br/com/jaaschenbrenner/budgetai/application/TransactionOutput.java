package br.com.jaaschenbrenner.budgetai.application;

import br.com.jaaschenbrenner.budgetai.domain.Category;
import br.com.jaaschenbrenner.budgetai.domain.Transaction;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionOutput(UUID id, String description, BigDecimal amount, Category category, LocalDateTime createdAt) {
    public static TransactionOutput from(Transaction transaction) {
        BigDecimal amount = BigDecimal.valueOf(transaction.amountInCents())
                .movePointLeft(2)
                .setScale(2, RoundingMode.HALF_UP);
        return new TransactionOutput(
                transaction.id().value(),
                transaction.description(),
                amount,
                transaction.category(),
                transaction.createdAt());
    }
}
