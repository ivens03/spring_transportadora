package br.com.fametro.spring.transportadora.usuario.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Cadastro de empresa cliente (POST /api/v1/clientes). Cria o login do Portal do Cliente (e-mail + senha, ativado
 * com o código devolvido) e guarda o endereço, que vira o destino padrão das entregas para a empresa.
 */
@Schema(description = "Empresa cliente: dados da empresa, login do portal e endereço de entrega")
public record ClienteRequest(
        @Schema(example = "Academia Gaviões Ltda") @NotBlank(message = "Informe o nome da empresa.")
        @Size(max = 150, message = "O nome tem no máximo 150 caracteres.")
        String nome,
        @Schema(description = "Com ou sem pontuação; aceita o CNPJ alfanumérico", example = "12.345.678/0001-95")
        @NotBlank(message = "Informe o CNPJ.")
        String cnpj,
        @Schema(description = "Login do Portal do Cliente", example = "logistica@academiagavioes.com.br")
        @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") @Size(max = 200, message = "O e-mail tem no máximo 200 caracteres.")
        String email,
        @Schema(example = "85999990000") @Pattern(regexp = "^[0-9]{10,11}$", message = "Telefone: DDD + número, só dígitos (10 ou 11).")
        String telefone,
        @Schema(example = "60325001") @Pattern(regexp = "^[0-9]{8}$", message = "CEP: 8 dígitos, sem hífen.")
        String cep,
        @Schema(example = "Av. Bezerra de Menezes, 1496") @NotBlank(message = "Informe o endereço.")
        @Size(max = 200, message = "O endereço tem no máximo 200 caracteres.")
        String endereco,
        @Schema(example = "São Gerardo") @NotBlank(message = "Informe o bairro.") @Size(max = 80, message = "O bairro tem no máximo 80 caracteres.")
        String bairro,
        @Schema(example = "Fortaleza") @NotBlank(message = "Informe a cidade.") @Size(max = 80, message = "A cidade tem no máximo 80 caracteres.")
        String cidade,
        @Schema(example = "CE") @NotBlank(message = "Informe a UF.") @Pattern(regexp = "(?i)^[A-Z]{2}$", message = "UF inválida.")
        String uf,
        @Schema(example = "-3.733676") @NotNull(message = "Marque o local da empresa no mapa (latitude).")
        @DecimalMin(value = "-90", message = "Latitude inválida.") @DecimalMax(value = "90", message = "Latitude inválida.")
        @Digits(integer = 2, fraction = 6, message = "Latitude com até 6 casas decimais.")
        BigDecimal latitude,
        @Schema(example = "-38.558170") @NotNull(message = "Marque o local da empresa no mapa (longitude).")
        @DecimalMin(value = "-180", message = "Longitude inválida.") @DecimalMax(value = "180", message = "Longitude inválida.")
        @Digits(integer = 3, fraction = 6, message = "Longitude com até 6 casas decimais.")
        BigDecimal longitude) {
}
