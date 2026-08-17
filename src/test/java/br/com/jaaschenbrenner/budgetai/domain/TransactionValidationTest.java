package br.com.jaaschenbrenner.budgetai.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionValidationTest {

    @Test
    void normalizesDescriptionAndKeepsValidAmount() {
        Transaction transaction = Transaction.create("  Café  ", 1250, Category.ALIMENTACAO);

        assertThat(transaction.description()).isEqualTo("Café");
        assertThat(transaction.amountInCents()).isEqualTo(1250);
    }

    @Test
    void rejectsInvalidBusinessInputsEvenWhenCalledByAiTool() {
        assertThatThrownBy(() -> Transaction.create("", 1250, Category.ALIMENTACAO))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Transaction.create("Café", 0, Category.ALIMENTACAO))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Transaction.create("x".repeat(Transaction.MAX_DESCRIPTION_LENGTH + 1), 1250, Category.ALIMENTACAO))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Transaction.create("Café", Transaction.MAX_AMOUNT_IN_CENTS + 1, Category.ALIMENTACAO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
