package br.com.jaaschenbrenner.budgetai.infrastructure.web;

import br.com.jaaschenbrenner.budgetai.domain.Category;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionCategoryUiContractTest {

    @Test
    void manualTransactionUiMustUseDomainCategoryValues() throws Exception {
        String html = resource("/static/index.html");
        String js = resource("/static/app.js");

        for (Category category : Category.values()) {
            assertThat(html).contains("value=\"" + category.name() + "\"");
        }

        assertThat(js).contains("/api/transactions/categories");
        assertThat(html).doesNotContain("<option>TRANSPORT</option>");
        assertThat(html).doesNotContain("<option>GROCERIES</option>");
    }

    private String resource(String path) throws Exception {
        try (InputStream input = getClass().getResourceAsStream(path)) {
            assertThat(input).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
