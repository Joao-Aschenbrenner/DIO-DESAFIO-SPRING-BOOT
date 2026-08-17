package br.com.jaaschenbrenner.budgetai.infrastructure.ai;

import br.com.jaaschenbrenner.budgetai.application.CreateTransactionInput;
import br.com.jaaschenbrenner.budgetai.application.CreateTransactionUseCase;
import br.com.jaaschenbrenner.budgetai.application.ListTransactionsUseCase;
import br.com.jaaschenbrenner.budgetai.domain.Category;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinanceToolsTest {

    @Test
    void convertsReaisToCentsAndNormalizesCategory() {
        CreateTransactionUseCase create = mock(CreateTransactionUseCase.class);
        ListTransactionsUseCase list = mock(ListTransactionsUseCase.class);
        when(create.execute(any())).thenReturn(null);
        FinanceTools tools = new FinanceTools(create, list);

        tools.registrarDespesa("Mercado", 42.35, "groceries");

        ArgumentCaptor<CreateTransactionInput> captor = ArgumentCaptor.forClass(CreateTransactionInput.class);
        verify(create).execute(captor.capture());
        assertThat(captor.getValue().description()).isEqualTo("Mercado");
        assertThat(captor.getValue().amountInCents()).isEqualTo(4235L);
        assertThat(captor.getValue().category()).isEqualTo(Category.ALIMENTACAO);
    }

    @Test
    void rejectsInvalidValuesBeforeCallingUseCase() {
        FinanceTools tools = new FinanceTools(mock(CreateTransactionUseCase.class), mock(ListTransactionsUseCase.class));
        assertThatThrownBy(() -> tools.registrarDespesa("Teste", 0, "OUTROS"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maior que zero");
    }
}
