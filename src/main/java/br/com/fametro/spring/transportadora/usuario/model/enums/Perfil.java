package br.com.fametro.spring.transportadora.usuario.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Perfil de acesso = folha do fluxo "Solicitação de Nova Matrícula" (docs/fluxo-acesso.mmd).
 * Cada perfil tem um vínculo, um tipo de credencial, um módulo e as permissões daquele módulo.
 */
@Getter
@RequiredArgsConstructor
public enum Perfil {

    ENTREGADOR(Vinculo.EXTERNO, TipoCredencial.APP_ENTREGADOR, "ENT", "App do Entregador",
            List.of("entrega:ver-atribuidas", "entrega:atualizar-status", "posicao:sincronizar")),

    ADMINISTRATIVO(Vinculo.INTERNO, TipoCredencial.PAINEL_WEB, "FUN", "Gestão & Cadastros Base",
            List.of("usuario:gerenciar", "motorista:cadastrar", "veiculo:cadastrar", "cliente:cadastrar")),

    SUPORTE(Vinculo.INTERNO, TipoCredencial.PAINEL_WEB, "FUN", "Operação Diária & Despacho",
            List.of("entrega:cadastrar", "mapa:acompanhar", "ocorrencia:tratar")),

    FINANCEIRO(Vinculo.INTERNO, TipoCredencial.PAINEL_WEB, "FUN", "Financeiro & Precificação",
            List.of("frete:analisar-gratis", "desconto:definir-por-rota", "faturamento:consultar")),

    DESENVOLVIMENTO(Vinculo.INTERNO, TipoCredencial.PAINEL_WEB, "FUN", "Monitoramento Técnico",
            List.of("mensageria:monitorar", "dispositivo:consultar-logs"));

    private final Vinculo vinculo;
    private final TipoCredencial tipoCredencial;
    private final String prefixoMatricula;
    private final String modulo;
    private final List<String> permissoes;

    /** Departamento correspondente (nulo para o entregador, que não tem departamento). */
    public Departamento departamento() {
        return vinculo == Vinculo.INTERNO ? Departamento.valueOf(name()) : null;
    }
}
