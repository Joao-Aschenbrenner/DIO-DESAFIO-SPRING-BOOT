package br.com.jaaschenbrenner.budgetai.infrastructure.config;

import br.com.jaaschenbrenner.budgetai.application.CreateTransactionUseCase;
import br.com.jaaschenbrenner.budgetai.application.ListTransactionsUseCase;
import br.com.jaaschenbrenner.budgetai.domain.TransactionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfiguration {
    @Bean
    CreateTransactionUseCase createTransactionUseCase(TransactionRepository repository) {
        return new CreateTransactionUseCase(repository);
    }

    @Bean
    ListTransactionsUseCase listTransactionsUseCase(TransactionRepository repository) {
        return new ListTransactionsUseCase(repository);
    }
}
