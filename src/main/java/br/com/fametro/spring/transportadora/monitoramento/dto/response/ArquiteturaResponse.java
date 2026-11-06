package br.com.fametro.spring.transportadora.monitoramento.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * "Como funciona" do sistema: o mapa (escrito à mão, em {@code arquitetura/arquitetura.json}) e o ambiente medido
 * agora no servidor. O painel web e o app desenham a mesma resposta.
 */
public record ArquiteturaResponse(Ambiente ambiente, List<Camada> camadas, List<Fluxo> fluxos, List<Ferramenta> ferramentas) {

    /** Mapa sem o ambiente: é o que vem do JSON. */
    public record Mapa(List<Camada> camadas, List<Fluxo> fluxos, List<Ferramenta> ferramentas) {
    }

    @Schema(description = "Lido do servidor no momento da chamada")
    public record Ambiente(
            @Schema(example = "homolog") String perfil,
            @Schema(example = "25.0.1 (Eclipse Adoptium)") String java,
            @Schema(example = "4.1.2") String springBoot,
            @Schema(example = "7.1.0.Final") String hibernate,
            @Schema(example = "PostgreSQL 17.6") String banco,
            @Schema(example = "6 — empresa cliente") String migracao,
            @Schema(example = "Linux aarch64 · 4 CPUs · 1024 MB para a JVM") String servidor,
            Instant noArDesde,
            Instant agora) {
    }

    /**
     * Uma caixa do desenho.
     * @param grupo {@code uso} (o caminho do dado quando alguém usa o sistema) ou {@code entrega} (do código ao ar)
     */
    public record Camada(String id, String grupo, String nome, String onde, String papel, List<Tecnologia> tecnologias) {
    }

    public record Tecnologia(String nome, String versao, String papel) {
    }

    /** Um caminho de dados, passo a passo; {@code de}/{@code para} são ids de {@link Camada}. */
    public record Fluxo(String id, String titulo, String resumo, List<Passo> passos) {
    }

    public record Passo(String de, String para, String oque) {
    }

    public record Ferramenta(String nome, String categoria, String para) {
    }
}
