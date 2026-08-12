package br.com.jaaschenbrenner.budgetai.domain;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository {
    Transaction save(Transaction transaction);
    List<Transaction> findAll();
    List<Transaction> findByCategory(Category category);
    Optional<Transaction> findById(TransactionId id);
}
