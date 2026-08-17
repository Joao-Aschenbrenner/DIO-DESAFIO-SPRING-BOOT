package br.com.jaaschenbrenner.budgetai.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryTest {

    @Test
    void normalizesPortugueseAndEnglishAliases() {
        assertThat(Category.fromExternalValue("alimentação")).isEqualTo(Category.ALIMENTACAO);
        assertThat(Category.fromExternalValue("GROCERIES")).isEqualTo(Category.ALIMENTACAO);
        assertThat(Category.fromExternalValue("TRANSPORT")).isEqualTo(Category.TRANSPORTE);
        assertThat(Category.fromExternalValue("farmácia")).isEqualTo(Category.SAUDE);
        assertThat(Category.fromExternalValue("entertainment")).isEqualTo(Category.LAZER);
        assertThat(Category.fromExternalValue("education")).isEqualTo(Category.EDUCACAO);
    }

    @Test
    void unknownCategoryFallsBackToOutros() {
        assertThat(Category.fromExternalValue("categoria inventada")).isEqualTo(Category.OUTROS);
        assertThat(Category.fromExternalValue(null)).isEqualTo(Category.OUTROS);
    }

    @Test
    void listFilterAcceptsAllAsNull() {
        assertThat(Category.fromExternalValueOrNull("TODAS")).isNull();
        assertThat(Category.fromExternalValueOrNull("all")).isNull();
        assertThat(Category.fromExternalValueOrNull("")).isNull();
    }
}
