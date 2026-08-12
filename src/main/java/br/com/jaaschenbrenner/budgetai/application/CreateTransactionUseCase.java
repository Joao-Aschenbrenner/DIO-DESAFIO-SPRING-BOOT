package br.com.jaaschenbrenner.budgetai.application;

import br.com.jaaschenbrenner.budgetai.domain.Transaction;
import br.com.jaaschenbrenner.budgetai.domain.TransactionRepository;

public class CreateTransactionUseCase {
    private final TransactionRepository repository;

    public CreateTransactionUseCase(TransactionRepository repository) {
        this.repository = repository;
    }

    public TransactionOutput execute(CreateTransactionInput input) {
        Transaction transaction = Transaction.create(input.description(), input.amountInCents(), input.category());
        return TransactionOutput.from(repository.save(transaction));
    }
}
