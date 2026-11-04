package br.com.fametro.spring.transportadora.carga.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = """
        Cadastro de carga. As coordenadas do destino são obrigatórias (o front obtém pelo mapa ou geocodificação):
        é com elas que a rota é otimizada.""")
public record CargaRequest(
        @Schema(example = "Caixa de eletrônicos") @NotBlank(message = "Informe a descrição.")
        @Size(max = 200, message = "A descrição tem no máximo 200 caracteres.")
        String descricao,
        @Schema(example = "12.5") @NotNull(message = "Informe o peso.")
        @DecimalMin(value = "0.01", message = "O peso deve ser maior que zero.")
        @Digits(integer = 8, fraction = 2, message = "Peso inválido.")
        BigDecimal pesoKg,
        @Schema(example = "0.08") @NotNull(message = "Informe o volume.")
        @DecimalMin(value = "0.001", message = "O volume deve ser maior que zero.")
        @Digits(integer = 5, fraction = 3, message = "Volume inválido.")
        BigDecimal volumeM3,
        @Schema(example = "Ana Ribeiro") @NotBlank(message = "Informe o destinatário.")
        @Size(max = 150, message = "O destinatário tem no máximo 150 caracteres.")
        String destinatarioNome,
        @Schema(example = "85999990000") @Pattern(regexp = "^[0-9]{10,13}$", message = "Telefone: só dígitos (10 a 13).")
        String destinatarioTelefone,
        @Schema(example = "Av. Santos Dumont, 1500") @NotBlank(message = "Informe o endereço.")
        @Size(max = 200, message = "O endereço tem no máximo 200 caracteres.")
        String endereco,
        @Schema(example = "Aldeota") @NotBlank(message = "Informe o bairro.") @Size(max = 80, message = "O bairro tem no máximo 80 caracteres.")
        String bairro,
        @Schema(example = "Fortaleza") @NotBlank(message = "Informe a cidade.") @Size(max = 80, message = "A cidade tem no máximo 80 caracteres.")
        String cidade,
        @Schema(example = "CE") @NotBlank(message = "Informe a UF.") @Pattern(regexp = "(?i)^[A-Z]{2}$", message = "UF inválida.")
        String uf,
        @Schema(example = "60150160") @Pattern(regexp = "^[0-9]{8}$", message = "CEP: 8 dígitos, sem hífen.")
        String cep,
        @Schema(example = "-3.737400") @NotNull(message = "Informe a latitude.")
        @DecimalMin(value = "-90", message = "Latitude inválida.") @DecimalMax(value = "90", message = "Latitude inválida.")
        @Digits(integer = 2, fraction = 6, message = "Latitude com até 6 casas decimais.")
        BigDecimal latitude,
        @Schema(example = "-38.497700") @NotNull(message = "Informe a longitude.")
        @DecimalMin(value = "-180", message = "Longitude inválida.") @DecimalMax(value = "180", message = "Longitude inválida.")
        @Digits(integer = 3, fraction = 6, message = "Longitude com até 6 casas decimais.")
        BigDecimal longitude) {
}
