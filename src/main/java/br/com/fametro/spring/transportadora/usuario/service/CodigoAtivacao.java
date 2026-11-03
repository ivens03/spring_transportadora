package br.com.fametro.spring.transportadora.usuario.service;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * Código de uso único entregue à pessoa para ativar o usuário (no banco fica só o hash).
 * Gerado no cadastro ({@code UsuarioService}) e conferido na ativação ({@code AutenticacaoService}).
 */
public final class CodigoAtivacao {

    /** Sem 0/O, 1/I/L para não confundir quem digita no celular. */
    private static final char[] ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int TAMANHO = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private CodigoAtivacao() {
    }

    public static String gerar() {
        char[] codigo = new char[TAMANHO];
        for (int i = 0; i < TAMANHO; i++) {
            codigo[i] = ALFABETO[RANDOM.nextInt(ALFABETO.length)];
        }
        return new String(codigo);
    }

    public static String normalizar(String codigo) {
        return codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT);
    }
}
