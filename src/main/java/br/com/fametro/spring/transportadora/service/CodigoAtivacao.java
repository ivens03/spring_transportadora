package br.com.fametro.spring.transportadora.service;

import java.security.SecureRandom;
import java.util.Locale;

/** Código de uso único entregue à pessoa para ativar o usuário (no banco fica só o hash). */
final class CodigoAtivacao {

    /** Sem 0/O, 1/I/L para não confundir quem digita no celular. */
    private static final char[] ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int TAMANHO = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private CodigoAtivacao() {
    }

    static String gerar() {
        char[] codigo = new char[TAMANHO];
        for (int i = 0; i < TAMANHO; i++) {
            codigo[i] = ALFABETO[RANDOM.nextInt(ALFABETO.length)];
        }
        return new String(codigo);
    }

    static String normalizar(String codigo) {
        return codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT);
    }
}
