package br.com.fametro.spring.transportadora.acesso.dominio;

import br.com.fametro.spring.transportadora.comum.ApiException;

import java.util.List;

/**
 * Perfil de acesso = folha do fluxo "Solicitação de Nova Matrícula" (docs/fluxo-acesso.mmd).
 * Cada perfil tem um vínculo, um tipo de credencial, um módulo e as permissões daquele módulo.
 */
public enum Perfil {

    ENTREGADOR(Vinculo.EXTERNO, TipoCredencial.APP_ENTREGADOR, "ENT", "App do Entregador",
            List.of("entrega:ver-atribuidas", "entrega:atualizar-status", "posicao:sincronizar")),

    ADMINISTRATIVO(Vinculo.INTERNO, TipoCredencial.PAINEL_WEB, "FUN", "Gestão & Cadastros Base",
            List.of("matricula:gerenciar", "motorista:cadastrar", "veiculo:cadastrar", "cliente:cadastrar")),

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

    Perfil(Vinculo vinculo, TipoCredencial tipoCredencial, String prefixoMatricula, String modulo, List<String> permissoes) {
        this.vinculo = vinculo;
        this.tipoCredencial = tipoCredencial;
        this.prefixoMatricula = prefixoMatricula;
        this.modulo = modulo;
        this.permissoes = permissoes;
    }

    /** Aplica as duas decisões do fluxo: "Qual o vínculo?" e, se interno, "Qual o departamento?". */
    public static Perfil de(Vinculo vinculo, Departamento departamento) {
        if (vinculo == Vinculo.EXTERNO) {
            if (departamento != null) {
                throw ApiException.requisicaoInvalida("Entregador (vínculo EXTERNO) não tem departamento.");
            }
            return ENTREGADOR;
        }
        if (departamento == null) {
            throw ApiException.requisicaoInvalida("Funcionário (vínculo INTERNO) precisa informar o departamento.");
        }
        return departamento.perfil();
    }

    public Departamento departamento() {
        return vinculo == Vinculo.INTERNO ? Departamento.valueOf(name()) : null;
    }

    public Vinculo getVinculo() { return vinculo; }
    public TipoCredencial getTipoCredencial() { return tipoCredencial; }
    public String getPrefixoMatricula() { return prefixoMatricula; }
    public String getModulo() { return modulo; }
    public List<String> getPermissoes() { return permissoes; }
}
