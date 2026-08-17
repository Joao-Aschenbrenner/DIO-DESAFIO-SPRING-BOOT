package br.com.jaaschenbrenner.budgetai.infrastructure.ai;

import br.com.jaaschenbrenner.budgetai.application.CreateTransactionInput;
import br.com.jaaschenbrenner.budgetai.application.CreateTransactionUseCase;
import br.com.jaaschenbrenner.budgetai.application.ListTransactionsUseCase;
import br.com.jaaschenbrenner.budgetai.application.TransactionOutput;
import br.com.jaaschenbrenner.budgetai.domain.Category;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Tool(description = "Registra uma nova despesa financeira. Informe descricao, valor em reais (por exemplo 42.50) e uma categoria. Categorias: ALIMENTACAO, TRANSPORTE, SAUDE, LAZER, MORADIA, EDUCACAO ou OUTROS.")
    public TransactionOutput registrarDespesa(String descricao, double valorEmReais, String categoria) {
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("A descrição da despesa é obrigatória.");
        }
        if (!Double.isFinite(valorEmReais) || valorEmReais <= 0) {
            throw new IllegalArgumentException("O valor da despesa deve ser maior que zero.");
        }

        long valorEmCentavos;
        try {
            valorEmCentavos = BigDecimal.valueOf(valorEmReais)
                    .setScale(2, RoundingMode.HALF_UP)
                    .movePointRight(2)
                    .longValueExact();
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("O valor informado é grande demais para ser registrado.", ex);
        }

        Category normalizedCategory = Category.fromExternalValue(categoria);
        return createTransactionUseCase.execute(new CreateTransactionInput(
                descricao.trim(), valorEmCentavos, normalizedCategory));
    }

    @Tool(description = "Lista despesas financeiras. A categoria é opcional; use TODAS para listar tudo. Categorias: ALIMENTACAO, TRANSPORTE, SAUDE, LAZER, MORADIA, EDUCACAO ou OUTROS.")
    public List<TransactionOutput> listarDespesas(String categoria) {
        return listTransactionsUseCase.execute(Category.fromExternalValueOrNull(categoria));
    }
}
