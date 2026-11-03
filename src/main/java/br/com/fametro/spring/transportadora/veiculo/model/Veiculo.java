package br.com.fametro.spring.transportadora.veiculo.model;

import br.com.fametro.spring.transportadora.veiculo.model.enums.Combustivel;
import br.com.fametro.spring.transportadora.veiculo.model.enums.StatusVeiculo;
import br.com.fametro.spring.transportadora.veiculo.model.enums.TipoVeiculo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;

/** Veículo da frota da transportadora. */
@Entity
@Table(name = "veiculo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Maiúscula, sem hífen: ABC1234 (antiga) ou ABC1D23 (Mercosul). */
    @Column(nullable = false, unique = true)
    private String placa;

    @Column(unique = true, updatable = false)
    private String renavam;

    @Column(nullable = false)
    private String marca;

    @Column(nullable = false)
    private String modelo;

    @Column(nullable = false, updatable = false)
    private Integer anoFabricacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TipoVeiculo tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Combustivel combustivel;

    @Column(nullable = false)
    private BigDecimal capacidadeCargaKg;

    @Column(nullable = false)
    private BigDecimal capacidadeVolumeM3;

    /** km/l esperado (fabricante ou histórico). Base para o alerta de consumo do Financeiro. */
    private BigDecimal consumoReferenciaKmPorLitro;

    @Column(nullable = false)
    private Integer quilometragemAtual;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusVeiculo status;

    /** Matrícula de quem cadastrou. */
    @Column(nullable = false, updatable = false)
    private String cadastradoPor;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(nullable = false)
    private Instant atualizadoEm;

    public static Veiculo novo(String placa, String renavam, String marca, String modelo, Integer anoFabricacao,
                               TipoVeiculo tipo, Combustivel combustivel, BigDecimal capacidadeCargaKg,
                               BigDecimal capacidadeVolumeM3, BigDecimal consumoReferenciaKmPorLitro,
                               Integer quilometragemAtual, String cadastradoPor, Instant agora) {
        Veiculo v = new Veiculo();
        v.placa = placa;
        v.renavam = renavam;
        v.marca = marca;
        v.modelo = modelo;
        v.anoFabricacao = anoFabricacao;
        v.tipo = tipo;
        v.combustivel = combustivel;
        v.capacidadeCargaKg = capacidadeCargaKg;
        v.capacidadeVolumeM3 = capacidadeVolumeM3;
        v.consumoReferenciaKmPorLitro = consumoReferenciaKmPorLitro;
        v.quilometragemAtual = quilometragemAtual == null ? 0 : quilometragemAtual;
        v.status = StatusVeiculo.ATIVO;
        v.cadastradoPor = cadastradoPor;
        v.criadoEm = agora;
        v.atualizadoEm = agora;
        return v;
    }

    /** "abc-1d23" -> "ABC1D23". */
    public static String normalizarPlaca(String placa) {
        return placa == null ? "" : placa.replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT);
    }

    public void alterarPlaca(String placa, Instant agora) { this.placa = placa; tocar(agora); }
    public void alterarMarca(String marca, Instant agora) { this.marca = marca; tocar(agora); }
    public void alterarModelo(String modelo, Instant agora) { this.modelo = modelo; tocar(agora); }
    public void alterarCapacidadeCargaKg(BigDecimal valor, Instant agora) { this.capacidadeCargaKg = valor; tocar(agora); }
    public void alterarCapacidadeVolumeM3(BigDecimal valor, Instant agora) { this.capacidadeVolumeM3 = valor; tocar(agora); }
    public void alterarConsumoReferencia(BigDecimal valor, Instant agora) { this.consumoReferenciaKmPorLitro = valor; tocar(agora); }
    public void alterarStatus(StatusVeiculo status, Instant agora) { this.status = status; tocar(agora); }
    public void alterarQuilometragem(Integer km, Instant agora) { this.quilometragemAtual = km; tocar(agora); }

    /** Odômetro só anda para frente (abastecimento antigo que chega atrasado não volta o km). */
    public void registrarOdometro(int odometroKm, Instant agora) {
        if (odometroKm > quilometragemAtual) {
            alterarQuilometragem(odometroKm, agora);
        }
    }

    public boolean inativo() {
        return status == StatusVeiculo.INATIVO;
    }

    private void tocar(Instant agora) {
        this.atualizadoEm = agora;
    }
}
