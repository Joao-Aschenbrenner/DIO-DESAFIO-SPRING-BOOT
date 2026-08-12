package br.com.jaaschenbrenner.budgetai.infrastructure.persistence;

import br.com.jaaschenbrenner.budgetai.domain.Category;
import br.com.jaaschenbrenner.budgetai.domain.Transaction;
import br.com.jaaschenbrenner.budgetai.domain.TransactionId;
import br.com.jaaschenbrenner.budgetai.domain.TransactionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaTransactionRepositoryAdapter implements TransactionRepository {
    private final SpringDataTransactionRepository repository;

    public JpaTransactionRepositoryAdapter(SpringDataTransactionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionEntity saved = repository.save(toEntity(transaction));
        return toDomain(saved);
    }

    @Override
    public List<Transaction> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Transaction> findByCategory(Category category) {
        return repository.findByCategory(category).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Transaction> findById(TransactionId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    private TransactionEntity toEntity(Transaction transaction) {
        return new TransactionEntity(
                transaction.id().value(),
                transaction.description(),
                transaction.amountInCents(),
                transaction.category(),
                transaction.createdAt());
    }

    private Transaction toDomain(TransactionEntity entity) {
        return new Transaction(
                new TransactionId(entity.getId()),
                entity.getDescription(),
                entity.getAmountInCents(),
                entity.getCategory(),
                entity.getCreatedAt());
    }
}
