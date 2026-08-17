package br.com.jaaschenbrenner.budgetai.application;

import br.com.jaaschenbrenner.budgetai.domain.Category;
import br.com.jaaschenbrenner.budgetai.domain.TransactionRepository;
import java.util.List;

public class ListTransactionsUseCase {
    private final TransactionRepository repository;

    public ListTransactionsUseCase(TransactionRepository repository) {
        this.repository = repository;
    }

    public List<TransactionOutput> execute(Category category) {
        return (category == null ? repository.findAll() : repository.findByCategory(category))
                .stream()
                .map(TransactionOutput::from)
                .toList();
    }
}
