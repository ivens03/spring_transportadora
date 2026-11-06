package br.com.fametro.spring.transportadora.usuario.model;

import java.util.Locale;

/**
 * CNPJ com dígitos verificadores, já no formato alfanumérico (Receita Federal, a partir de julho/2026):
 * 12 caracteres de identificação (letras A-Z ou números) + 2 dígitos verificadores numéricos.
 * O cálculo é o mesmo módulo 11 de sempre; cada caractere vale o código ASCII menos 48
 * ('0'..'9' = 0..9, 'A' = 17, 'B' = 18 ...), então os CNPJs só com números continuam válidos.
 */
public final class Cnpj {

    private static final int[] PESOS_DV1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_DV2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private Cnpj() {
    }

    /** Tira pontuação e espaços e põe em maiúsculas: "12.ABC.345/01DE-35" → "12ABC34501DE35". */
    public static String normalizar(String cnpj) {
        return cnpj == null ? "" : cnpj.replaceAll("[.\\-/\\s]", "").toUpperCase(Locale.ROOT);
    }

    /** Recebe com ou sem pontuação. */
    public static boolean valido(String cnpj) {
        String c = normalizar(cnpj);
        if (!c.matches("[0-9A-Z]{12}[0-9]{2}") || c.chars().allMatch(ch -> ch == c.charAt(0))) {
            return false;
        }
        int dv1 = digito(c.substring(0, 12), PESOS_DV1);
        int dv2 = digito(c.substring(0, 12) + dv1, PESOS_DV2);
        return c.charAt(12) - '0' == dv1 && c.charAt(13) - '0' == dv2;
    }

    private static int digito(String base, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < base.length(); i++) {
            soma += (base.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    /** "12345678000195" → "12.345.678/0001-95" (também para o alfanumérico). */
    public static String formatar(String cnpj) {
        String c = normalizar(cnpj);
        if (c.length() != 14) {
            return cnpj;
        }
        return c.substring(0, 2) + "." + c.substring(2, 5) + "." + c.substring(5, 8) + "/" + c.substring(8, 12) + "-" + c.substring(12);
    }
}
