package br.com.jaaschenbrenner.budgetai.infrastructure.web;

import br.com.jaaschenbrenner.budgetai.domain.Category;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionCategoryUiContractTest {

    @Test
    void manualTransactionUiMustUseDomainCategoryValues() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/static/index.html")) {
            assertThat(input).isNotNull();
            String html = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            for (Category category : Category.values()) {
                assertThat(html).contains("value=\"" + category.name() + "\"");
            }

            assertThat(html).contains("/api/transactions/categories");
            assertThat(html).doesNotContain("<option>TRANSPORT</option>");
            assertThat(html).doesNotContain("<option>GROCERIES</option>");
        }
    }
}
