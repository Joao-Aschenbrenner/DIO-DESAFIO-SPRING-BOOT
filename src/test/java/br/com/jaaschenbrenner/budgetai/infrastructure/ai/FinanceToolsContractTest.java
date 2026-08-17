package br.com.jaaschenbrenner.budgetai.infrastructure.ai;

import br.com.jaaschenbrenner.budgetai.domain.Category;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.ToolParam;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class FinanceToolsContractTest {

    @Test
    void listCategoryMustBeOptionalForQueriesWithoutFilter() throws Exception {
        Method method = FinanceTools.class.getMethod("listarDespesas", Category.class);
        ToolParam annotation = method.getParameters()[0].getAnnotation(ToolParam.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.required()).isFalse();
    }
}
