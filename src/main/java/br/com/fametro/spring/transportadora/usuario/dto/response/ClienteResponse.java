package br.com.fametro.spring.transportadora.usuario.dto.response;

import br.com.fametro.spring.transportadora.usuario.model.DadosEmpresa;
import br.com.fametro.spring.transportadora.usuario.model.Usuario;
import br.com.fametro.spring.transportadora.usuario.model.enums.StatusUsuario;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Empresa cliente para o Suporte vincular à entrega (sem dados de acesso). O endereço é o destino padrão:
 * escolhida a empresa, a nova entrega já vem com ele. Campos da empresa são nulos em clientes antigos
 * (cadastrados antes do V6, só com nome e e-mail).
 */
public record ClienteResponse(
        @Schema(example = "CLI-000001") String matricula,
        @Schema(example = "Academia Gaviões Ltda") String nome,
        @Schema(example = "logistica@academiagavioes.com.br") String email,
        @Schema(example = "ATIVO") StatusUsuario status,
        @Schema(description = "Sem pontuação (pode ter letras: CNPJ alfanumérico)", example = "12345678000195") String cnpj,
        @Schema(example = "85999990000") String telefone,
        @Schema(example = "60325001") String cep,
        @Schema(example = "Av. Bezerra de Menezes, 1496") String endereco,
        @Schema(example = "São Gerardo") String bairro,
        @Schema(example = "Fortaleza") String cidade,
        @Schema(example = "CE") String uf,
        @Schema(example = "-3.733676") BigDecimal latitude,
        @Schema(example = "-38.558170") BigDecimal longitude) {

    public static ClienteResponse de(Usuario u) {
        DadosEmpresa e = u.getEmpresa() == null
                ? new DadosEmpresa(null, null, null, null, null, null, null, null, null) : u.getEmpresa();
        return new ClienteResponse(u.getMatricula(), u.getNome(), u.getEmail(), u.getStatus(), e.cnpj(), e.telefone(),
                e.cep(), e.endereco(), e.bairro(), e.cidade(), e.uf(), e.latitude(), e.longitude());
    }
}
