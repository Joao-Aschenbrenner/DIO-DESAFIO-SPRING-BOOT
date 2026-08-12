package br.com.jaaschenbrenner.budgetai.application;

import br.com.jaaschenbrenner.budgetai.domain.Category;
import br.com.jaaschenbrenner.budgetai.domain.Transaction;
import br.com.jaaschenbrenner.budgetai.domain.TransactionId;
import br.com.jaaschenbrenner.budgetai.domain.TransactionRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CreateTransactionUseCaseTest {
    @Test
    void criaTransacaoComValorEmCentavos() {
        InMemoryRepository repository = new InMemoryRepository();
        CreateTransactionUseCase useCase = new CreateTransactionUseCase(repository);

        TransactionOutput output = useCase.execute(
                new CreateTransactionInput("Café", 1250, Category.ALIMENTACAO));

        assertThat(output.description()).isEqualTo("Café");
        assertThat(output.amount()).hasToString("12.50");
        assertThat(output.category()).isEqualTo(Category.ALIMENTACAO);
        assertThat(repository.findAll()).hasSize(1);
    }

    private static final class InMemoryRepository implements TransactionRepository {
        private final List<Transaction> items = new ArrayList<>();

        @Override public Transaction save(Transaction transaction) { items.add(transaction); return transaction; }
        @Override public List<Transaction> findAll() { return List.copyOf(items); }
        @Override public List<Transaction> findByCategory(Category category) { return items.stream().filter(t -> t.category() == category).toList(); }
        @Override public Optional<Transaction> findById(TransactionId id) { return items.stream().filter(t -> t.id().equals(id)).findFirst(); }
    }
}
