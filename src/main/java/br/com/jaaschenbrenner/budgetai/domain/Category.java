package br.com.jaaschenbrenner.budgetai.domain;

import java.text.Normalizer;
import java.util.Locale;

public enum Category {
    ALIMENTACAO,
    TRANSPORTE,
    SAUDE,
    LAZER,
    MORADIA,
    EDUCACAO,
    OUTROS;

    public static Category fromExternalValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return OUTROS;
        }

        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        return switch (normalized) {
            case "ALIMENTACAO", "ALIMENTO", "ALIMENTOS", "COMIDA", "FOOD", "GROCERY", "GROCERIES",
                    "RESTAURANT", "RESTAURANTE", "MERCADO", "SUPERMERCADO" -> ALIMENTACAO;
            case "TRANSPORTE", "TRANSPORT", "AUTO", "CARRO", "UBER", "TAXI", "COMBUSTIVEL", "GASOLINA" -> TRANSPORTE;
            case "SAUDE", "HEALTH", "FARMACIA", "MEDICO", "MEDICAMENTO" -> SAUDE;
            case "LAZER", "ENTERTAINMENT", "ENTRETENIMENTO", "CINEMA", "VIAGEM" -> LAZER;
            case "MORADIA", "HOUSING", "CASA", "ALUGUEL", "RENT", "CONDOMINIO" -> MORADIA;
            case "EDUCACAO", "EDUCATION", "CURSO", "ESCOLA", "FACULDADE", "LIVRO" -> EDUCACAO;
            case "OUTROS", "OUTRO", "OTHER", "OTHERS" -> OUTROS;
            default -> {
                try {
                    yield Category.valueOf(normalized);
                } catch (IllegalArgumentException ignored) {
                    yield OUTROS;
                }
            }
        };
    }

    public static Category fromExternalValueOrNull(String raw) {
        if (raw == null || raw.isBlank() || "TODAS".equalsIgnoreCase(raw) || "TODOS".equalsIgnoreCase(raw)
                || "ALL".equalsIgnoreCase(raw)) {
            return null;
        }
        return fromExternalValue(raw);
    }
}
