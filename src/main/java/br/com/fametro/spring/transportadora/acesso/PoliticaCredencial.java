package br.com.fametro.spring.transportadora.acesso;

import br.com.fametro.spring.transportadora.acesso.dominio.TipoCredencial;
import br.com.fametro.spring.transportadora.comum.ApiException;

import java.util.regex.Pattern;

/** Regras do segredo que cada tipo de credencial aceita na ativação. */
final class PoliticaCredencial {

    private static final Pattern PIN = Pattern.compile("\\d{6}");
    private static final Pattern LETRA = Pattern.compile("\\p{L}");
    private static final Pattern DIGITO = Pattern.compile("\\d");
    static final int TAMANHO_MINIMO_SENHA = 10;

    private PoliticaCredencial() {
    }

    static void validar(TipoCredencial tipo, String segredo) {
        if (segredo == null || segredo.isBlank()) {
            throw ApiException.requisicaoInvalida("Informe a nova credencial.");
        }
        switch (tipo) {
            case APP_ENTREGADOR -> validarPin(segredo);
            case PAINEL_WEB -> validarSenha(segredo);
        }
    }

    /** App: PIN numérico de 6 dígitos (digitado no celular, na rua). Sem PIN óbvio. */
    private static void validarPin(String pin) {
        if (!PIN.matcher(pin).matches()) {
            throw ApiException.requisicaoInvalida("O PIN do app deve ter exatamente 6 dígitos numéricos.");
        }
        if (todosIguais(pin) || sequencial(pin)) {
            throw ApiException.requisicaoInvalida("PIN fácil demais (dígitos repetidos ou em sequência).");
        }
    }

    /** Painel: senha com pelo menos 10 caracteres, letras e números. */
    private static void validarSenha(String senha) {
        if (senha.length() < TAMANHO_MINIMO_SENHA || !LETRA.matcher(senha).find() || !DIGITO.matcher(senha).find()) {
            throw ApiException.requisicaoInvalida(
                    "A senha do painel deve ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres, com letras e números.");
        }
    }

    private static boolean todosIguais(String pin) {
        return pin.chars().distinct().count() == 1;
    }

    private static boolean sequencial(String pin) {
        boolean crescente = true;
        boolean decrescente = true;
        for (int i = 1; i < pin.length(); i++) {
            int diff = pin.charAt(i) - pin.charAt(i - 1);
            crescente &= diff == 1;
            decrescente &= diff == -1;
        }
        return crescente || decrescente;
    }
}
