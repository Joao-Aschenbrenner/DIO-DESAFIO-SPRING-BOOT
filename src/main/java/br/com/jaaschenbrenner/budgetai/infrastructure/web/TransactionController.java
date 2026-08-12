package br.com.jaaschenbrenner.budgetai.infrastructure.web;

import br.com.jaaschenbrenner.budgetai.application.CreateTransactionInput;
import br.com.jaaschenbrenner.budgetai.application.CreateTransactionUseCase;
import br.com.jaaschenbrenner.budgetai.application.ListTransactionsUseCase;
import br.com.jaaschenbrenner.budgetai.application.TransactionOutput;
import br.com.jaaschenbrenner.budgetai.domain.Category;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final CreateTransactionUseCase createTransactionUseCase;
    private final ListTransactionsUseCase listTransactionsUseCase;

    public TransactionController(CreateTransactionUseCase createTransactionUseCase,
                                 ListTransactionsUseCase listTransactionsUseCase) {
        this.createTransactionUseCase = createTransactionUseCase;
        this.listTransactionsUseCase = listTransactionsUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionOutput create(@Valid @RequestBody CreateTransactionRequest request) {
        return createTransactionUseCase.execute(new CreateTransactionInput(
                request.description(), request.amountInCents(), request.category()));
    }

    @GetMapping
    public List<TransactionOutput> list(@RequestParam(required = false) Category category) {
        return listTransactionsUseCase.execute(category);
    }

    public record CreateTransactionRequest(
            @NotBlank String description,
            @Positive long amountInCents,
            @NotNull Category category) {}
}
