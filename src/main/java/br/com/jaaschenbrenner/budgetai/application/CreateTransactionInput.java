package br.com.jaaschenbrenner.budgetai.application;

import br.com.jaaschenbrenner.budgetai.domain.Category;

public record CreateTransactionInput(String description, long amountInCents, Category category) {}
