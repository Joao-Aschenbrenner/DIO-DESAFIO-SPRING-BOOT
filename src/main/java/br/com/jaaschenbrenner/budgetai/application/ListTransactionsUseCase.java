package br.com.jaaschenbrenner.budgetai.application;

import br.com.jaaschenbrenner.budgetai.domain.Category;
import br.com.jaaschenbrenner.budgetai.domain.Transaction;
import br.com.jaaschenbrenner.budgetai.domain.TransactionRepository;

import java.util.Comparator;
import java.util.List;

public class ListTransactionsUseCase {
    private final TransactionRepository repository;

    public ListTransactionsUseCase(TransactionRepository repository) {
        this.repository = repository;
    }

    public List<TransactionOutput> execute(Category category) {
        List<Transaction> transactions = category == null
                ? repository.findAll()
                : repository.findByCategory(category);

        return transactions.stream()
                .sorted(Comparator.comparing(Transaction::createdAt).reversed())
                .map(TransactionOutput::from)
                .toList();
    }
}
