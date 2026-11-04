package br.com.fametro.spring.transportadora.carga.dto.request;

import br.com.fametro.spring.transportadora.carga.model.enums.StatusCarga;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** PATCH: só os campos enviados mudam; só vale enquanto a carga está AGUARDANDO_ROTA. Endereço muda em bloco. */
@Schema(description = """
        Atualização parcial da carga (só enquanto AGUARDANDO_ROTA). Para mudar o destino envie endereco, bairro,
        cidade, uf, latitude e longitude juntos (cep opcional). status aceita apenas CANCELADA.""")
public record CargaPatchRequest(
        @Size(max = 200, message = "A descrição tem no máximo 200 caracteres.") String descricao,
        @DecimalMin(value = "0.01", message = "O peso deve ser maior que zero.")
        @Digits(integer = 8, fraction = 2, message = "Peso inválido.") BigDecimal pesoKg,
        @DecimalMin(value = "0.001", message = "O volume deve ser maior que zero.")
        @Digits(integer = 5, fraction = 3, message = "Volume inválido.") BigDecimal volumeM3,
        @Size(max = 150, message = "O destinatário tem no máximo 150 caracteres.") String destinatarioNome,
        @Pattern(regexp = "^[0-9]{10,13}$", message = "Telefone: só dígitos (10 a 13).") String destinatarioTelefone,
        @Size(max = 200, message = "O endereço tem no máximo 200 caracteres.") String endereco,
        @Size(max = 80, message = "O bairro tem no máximo 80 caracteres.") String bairro,
        @Size(max = 80, message = "A cidade tem no máximo 80 caracteres.") String cidade,
        @Pattern(regexp = "(?i)^[A-Z]{2}$", message = "UF inválida.") String uf,
        @Pattern(regexp = "^[0-9]{8}$", message = "CEP: 8 dígitos, sem hífen.") String cep,
        @DecimalMin(value = "-90", message = "Latitude inválida.") @DecimalMax(value = "90", message = "Latitude inválida.")
        @Digits(integer = 2, fraction = 6, message = "Latitude com até 6 casas decimais.") BigDecimal latitude,
        @DecimalMin(value = "-180", message = "Longitude inválida.") @DecimalMax(value = "180", message = "Longitude inválida.")
        @Digits(integer = 3, fraction = 6, message = "Longitude com até 6 casas decimais.") BigDecimal longitude,
        @Schema(description = "Somente CANCELADA", example = "CANCELADA") StatusCarga status) {

    public boolean vazio() {
        return descricao == null && pesoKg == null && volumeM3 == null && destinatarioNome == null
                && destinatarioTelefone == null && !mexeNoEndereco() && status == null;
    }

    public boolean mexeNoEndereco() {
        return endereco != null || bairro != null || cidade != null || uf != null || cep != null
                || latitude != null || longitude != null;
    }

    public boolean enderecoCompleto() {
        return endereco != null && bairro != null && cidade != null && uf != null && latitude != null && longitude != null;
    }
}
