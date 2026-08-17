package br.com.jaaschenbrenner.budgetai.infrastructure.ai;

import br.com.jaaschenbrenner.budgetai.application.CreateTransactionInput;
import br.com.jaaschenbrenner.budgetai.application.CreateTransactionUseCase;
import br.com.jaaschenbrenner.budgetai.application.ListTransactionsUseCase;
import br.com.jaaschenbrenner.budgetai.application.TransactionOutput;
import br.com.jaaschenbrenner.budgetai.domain.Category;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
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

    @Tool(description = """
            Registra uma nova despesa financeira real no sistema.
            Use quando a pessoa disser que gastou, pagou, comprou ou quiser registrar uma despesa.
            Categorias permitidas: ALIMENTACAO, TRANSPORTE, SAUDE, LAZER, MORADIA, EDUCACAO e OUTROS.
            Nunca invente categoria fora dessa lista.
            """)
    public TransactionOutput registrarDespesa(
            @ToolParam(description = "Descrição curta e objetiva da despesa, com no máximo 120 caracteres")
            String descricao,
            @ToolParam(description = "Valor inteiro em centavos. Exemplo: R$ 42,00 deve ser enviado como 4200")
            long valorEmCentavos,
            @ToolParam(description = "Categoria exata: ALIMENTACAO, TRANSPORTE, SAUDE, LAZER, MORADIA, EDUCACAO ou OUTROS")
            Category categoria) {
        return createTransactionUseCase.execute(new CreateTransactionInput(descricao, valorEmCentavos, categoria));
    }

    @Tool(description = """
            Consulta as despesas financeiras reais salvas no sistema.
            Use para perguntas como 'quais foram meus gastos?', 'liste as despesas' ou 'o que gastei com alimentação?'.
            A categoria é opcional; quando ausente, liste todas as despesas.
            """)
    public List<TransactionOutput> listarDespesas(
            @ToolParam(
                    description = "Filtro opcional: ALIMENTACAO, TRANSPORTE, SAUDE, LAZER, MORADIA, EDUCACAO ou OUTROS",
                    required = false)
            Category categoria) {
        return listTransactionsUseCase.execute(categoria);
    }
}
