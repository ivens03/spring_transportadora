package br.com.fametro.spring.transportadora.model.enums;

/**
 * Tipo de credencial que o usuário recebe. Define COMO a pessoa entra e ONDE o token vale.
 * <ul>
 *   <li>{@link #APP_ENTREGADOR}: login = matrícula + PIN de 6 dígitos; token longo (cobre a jornada,
 *       pensando na conexão instável); só vale em {@code /api/app/**}.</li>
 *   <li>{@link #PAINEL_WEB}: login = e-mail corporativo + senha forte; só vale em {@code /api/painel/**}.</li>
 * </ul>
 */
public enum TipoCredencial {
    APP_ENTREGADOR,
    PAINEL_WEB;

    /** Authority colocada no token para o Spring Security barrar uso cruzado (app x painel). */
    public String authority() {
        return "CRED_" + name();
    }
}
