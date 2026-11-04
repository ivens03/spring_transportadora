package br.com.fametro.spring.transportadora.usuario.model;

/**
 * Permissões que vão no token (claim "permissoes"). O {@code SecurityConfig} libera cada recurso por elas,
 * então dar uma permissão a outro perfil (em {@code Perfil}) já libera o acesso, sem mexer na segurança.
 */
public final class Permissoes {

    // App do entregador
    public static final String ENTREGA_VER_ATRIBUIDAS = "entrega:ver-atribuidas";
    public static final String ENTREGA_ATUALIZAR_STATUS = "entrega:atualizar-status";
    public static final String POSICAO_SINCRONIZAR = "posicao:sincronizar";

    // Portal do Cliente (só as encomendas/webhooks do próprio cliente)
    public static final String ENCOMENDA_ACOMPANHAR = "encomenda:acompanhar";
    public static final String WEBHOOK_GERENCIAR = "webhook:gerenciar";

    // Gestão & cadastros
    public static final String USUARIO_GERENCIAR = "usuario:gerenciar";
    public static final String MOTORISTA_CADASTRAR = "motorista:cadastrar";
    public static final String CLIENTE_CADASTRAR = "cliente:cadastrar";

    // Frota (ADMINISTRATIVO, SUPORTE e FINANCEIRO)
    public static final String VEICULO_GERENCIAR = "veiculo:gerenciar";
    public static final String ABASTECIMENTO_REGISTRAR = "abastecimento:registrar";
    /** Métricas de consumo da frota: só FINANCEIRO. */
    public static final String CONSUMO_VER_METRICAS = "consumo:ver-metricas";

    // Operação
    public static final String ENTREGA_CADASTRAR = "entrega:cadastrar";
    public static final String MAPA_ACOMPANHAR = "mapa:acompanhar";
    public static final String OCORRENCIA_TRATAR = "ocorrencia:tratar";

    // Financeiro
    public static final String FRETE_ANALISAR_GRATIS = "frete:analisar-gratis";
    public static final String DESCONTO_DEFINIR_POR_ROTA = "desconto:definir-por-rota";
    public static final String FATURAMENTO_CONSULTAR = "faturamento:consultar";

    // Monitoramento técnico
    public static final String MENSAGERIA_MONITORAR = "mensageria:monitorar";
    public static final String DISPOSITIVO_CONSULTAR_LOGS = "dispositivo:consultar-logs";

    private Permissoes() {
    }
}
