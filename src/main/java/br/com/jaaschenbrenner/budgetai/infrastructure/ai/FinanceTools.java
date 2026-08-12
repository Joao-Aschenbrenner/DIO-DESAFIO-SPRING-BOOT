package br.com.jaaschenbrenner.budgetai.infrastructure.ai;

import br.com.jaaschenbrenner.budgetai.application.CreateTransactionInput;
import br.com.jaaschenbrenner.budgetai.application.CreateTransactionUseCase;
import br.com.jaaschenbrenner.budgetai.application.ListTransactionsUseCase;
import br.com.jaaschenbrenner.budgetai.application.TransactionOutput;
import br.com.jaaschenbrenner.budgetai.domain.Category;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FinanceTools {
    private final CreateTransactionUseCase createTransactionUseCase;
    private final ListTransactionsUseCase listTransactionsUseCase;

    public FinanceTools(CreateTransactionUseCase createTransactionUseCase,
                        ListTransactionsUseCase listTransactionsUseCase) {
        this.createTransactionUseCase = createTransactionUseCase;
        this.listTransactionsUseCase = listTransactionsUseCase;
    }

    @Tool(description = "Registra uma nova despesa financeira. O valor deve ser informado em centavos e a categoria deve ser uma das categorias disponíveis no sistema.")
    public TransactionOutput registrarDespesa(String descricao, long valorEmCentavos, Category categoria) {
        return createTransactionUseCase.execute(new CreateTransactionInput(descricao, valorEmCentavos, categoria));
    }

    @Tool(description = "Lista despesas financeiras. Quando uma categoria for informada, retorna apenas despesas daquela categoria.")
    public List<TransactionOutput> listarDespesas(Category categoria) {
        return listTransactionsUseCase.execute(categoria);
    }
}
