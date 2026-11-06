package br.com.fametro.spring.transportadora.usuario.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static br.com.fametro.spring.transportadora.usuario.model.Permissoes.*;

/**
 * Perfil de acesso = folha do fluxo "Solicitação de Nova Matrícula" (docs/fluxo-acesso.mmd).
 * Cada perfil tem um vínculo, um tipo de credencial, um módulo e as permissões.
 * As permissões vão no token e é por elas que o SecurityConfig libera cada recurso.
 */
@Getter
@RequiredArgsConstructor
public enum Perfil {

    ENTREGADOR(Vinculo.EXTERNO, TipoCredencial.APP_ENTREGADOR, "ENT", "App do Entregador",
            List.of(ENTREGA_VER_ATRIBUIDAS, ENTREGA_ATUALIZAR_STATUS, POSICAO_SINCRONIZAR)),

    /** Entra como o painel (e-mail + senha), mas só enxerga as próprias encomendas e webhooks. */
    CLIENTE(Vinculo.CLIENTE, TipoCredencial.PAINEL_WEB, "CLI", "Portal do Cliente",
            List.of(ENCOMENDA_ACOMPANHAR, WEBHOOK_GERENCIAR)),

    ADMINISTRATIVO(Vinculo.INTERNO, TipoCredencial.PAINEL_WEB, "FUN", "Gestão & Cadastros Base",
            List.of(USUARIO_GERENCIAR, MOTORISTA_CADASTRAR, CLIENTE_CADASTRAR,
                    VEICULO_GERENCIAR, ABASTECIMENTO_REGISTRAR)),

    SUPORTE(Vinculo.INTERNO, TipoCredencial.PAINEL_WEB, "FUN", "Operação Diária & Despacho",
            List.of(ENTREGA_CADASTRAR, MAPA_ACOMPANHAR, OCORRENCIA_TRATAR, CLIENTE_CADASTRAR,
                    VEICULO_GERENCIAR, ABASTECIMENTO_REGISTRAR)),

    FINANCEIRO(Vinculo.INTERNO, TipoCredencial.PAINEL_WEB, "FUN", "Financeiro & Precificação",
            List.of(FRETE_ANALISAR_GRATIS, DESCONTO_DEFINIR_POR_ROTA, FATURAMENTO_CONSULTAR,
                    VEICULO_GERENCIAR, ABASTECIMENTO_REGISTRAR, CONSUMO_VER_METRICAS)),

    DESENVOLVIMENTO(Vinculo.INTERNO, TipoCredencial.PAINEL_WEB, "FUN", "Monitoramento Técnico",
            List.of(MENSAGERIA_MONITORAR, DISPOSITIVO_CONSULTAR_LOGS));

    private final Vinculo vinculo;
    private final TipoCredencial tipoCredencial;
    private final String prefixoMatricula;
    private final String modulo;
    private final List<String> permissoes;

    /** Quem entra pelo e-mail (painel/portal) precisa ter e-mail cadastrado. */
    public boolean exigeEmail() {
        return tipoCredencial == TipoCredencial.PAINEL_WEB;
    }

    /** Departamento correspondente (nulo para entregador e cliente, que não têm departamento). */
    public Departamento departamento() {
        return vinculo == Vinculo.INTERNO ? Departamento.valueOf(name()) : null;
    }
}
