package br.com.fametro.spring.transportadora.rota.model;

import br.com.fametro.spring.transportadora.carga.model.Carga;
import br.com.fametro.spring.transportadora.exception.custom.RegraNegocioException;
import br.com.fametro.spring.transportadora.rota.model.enums.StatusParada;
import br.com.fametro.spring.transportadora.rota.model.enums.StatusRota;
import br.com.fametro.spring.transportadora.usuario.model.Usuario;
import br.com.fametro.spring.transportadora.veiculo.model.Veiculo;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Rota de entrega: um veículo, um entregador e as paradas em ordem otimizada. */
@Entity
@Table(name = "rota")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ROT-000001. */
    @Column(nullable = false, unique = true, updatable = false)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "veiculo_id", nullable = false, updatable = false)
    private Veiculo veiculo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entregador_id", nullable = false, updatable = false)
    private Usuario entregador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusRota status;

    @Column(nullable = false, updatable = false)
    private BigDecimal origemLatitude;

    @Column(nullable = false, updatable = false)
    private BigDecimal origemLongitude;

    @Column(nullable = false, updatable = false)
    private boolean retornaOrigem;

    /** Estimativa pelas ruas (linha reta × fator viário), incluindo o retorno se houver. */
    @Column(nullable = false, updatable = false)
    private BigDecimal distanciaEstimadaKm;

    @Column(nullable = false, updatable = false)
    private Integer duracaoEstimadaMin;

    @Column(nullable = false, updatable = false)
    private BigDecimal pesoTotalKg;

    @Column(nullable = false, updatable = false)
    private BigDecimal volumeTotalM3;

    @Column(nullable = false, updatable = false)
    private String criadaPor;

    @Column(nullable = false, updatable = false)
    private Instant criadaEm;

    private Instant iniciadaEm;
    private Instant concluidaEm;
    private Instant canceladaEm;

    @Column(nullable = false)
    private Instant atualizadaEm;

    @OneToMany(mappedBy = "rota", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem")
    private List<ParadaRota> paradas = new ArrayList<>();

    public static Rota nova(String codigo, Veiculo veiculo, Usuario entregador, BigDecimal origemLatitude,
                            BigDecimal origemLongitude, boolean retornaOrigem, BigDecimal distanciaEstimadaKm,
                            int duracaoEstimadaMin, BigDecimal pesoTotalKg, BigDecimal volumeTotalM3,
                            String criadaPor, Instant agora) {
        Rota r = new Rota();
        r.codigo = codigo;
        r.veiculo = veiculo;
        r.entregador = entregador;
        r.status = StatusRota.PLANEJADA;
        r.origemLatitude = origemLatitude;
        r.origemLongitude = origemLongitude;
        r.retornaOrigem = retornaOrigem;
        r.distanciaEstimadaKm = distanciaEstimadaKm;
        r.duracaoEstimadaMin = duracaoEstimadaMin;
        r.pesoTotalKg = pesoTotalKg;
        r.volumeTotalM3 = volumeTotalM3;
        r.criadaPor = criadaPor;
        r.criadaEm = agora;
        r.atualizadaEm = agora;
        return r;
    }

    public void adicionarParada(Carga carga, int ordem, BigDecimal distanciaDoAnteriorKm) {
        paradas.add(ParadaRota.nova(this, carga, ordem, distanciaDoAnteriorKm));
    }

    /** Antes de recalcular a ordem: tira todas as paradas (a rota precisa estar PLANEJADA). */
    public void limparParadas() {
        exigir(StatusRota.PLANEJADA, "mudar as paradas de");
        paradas.clear();
    }

    /** Novas estimativas depois de incluir entregas numa rota que ainda não saiu. */
    public void replanejar(BigDecimal distanciaEstimadaKm, int duracaoEstimadaMin, BigDecimal pesoTotalKg,
                           BigDecimal volumeTotalM3, Instant agora) {
        exigir(StatusRota.PLANEJADA, "replanejar");
        this.distanciaEstimadaKm = distanciaEstimadaKm;
        this.duracaoEstimadaMin = duracaoEstimadaMin;
        this.pesoTotalKg = pesoTotalKg;
        this.volumeTotalM3 = volumeTotalM3;
        this.atualizadaEm = agora;
    }

    public boolean pertenceA(String matricula) {
        return entregador.getMatricula().equals(matricula);
    }

    /** Entregador sai do depósito: as cargas vão para EM_TRANSITO. */
    public void iniciar(Instant agora) {
        exigir(StatusRota.PLANEJADA, "iniciar");
        status = StatusRota.EM_ANDAMENTO;
        iniciadaEm = agora;
        paradas.forEach(p -> p.getCarga().embarcar(agora));
        atualizadaEm = agora;
    }

    /** Só antes de sair: as cargas voltam para a fila sem contar tentativa. */
    public void cancelar(Instant agora) {
        if (status == StatusRota.EM_ANDAMENTO) {
            throw new RegraNegocioException("Rota " + codigo + " já está EM_ANDAMENTO: baixe as paradas restantes como "
                    + "NAO_ENTREGUE para encerrá-la.");
        }
        exigir(StatusRota.PLANEJADA, "cancelar");
        status = StatusRota.CANCELADA;
        canceladaEm = agora;
        paradas.forEach(p -> p.getCarga().liberar(agora));
        atualizadaEm = agora;
    }

    public ParadaRota parada(int ordem) {
        return paradas.stream().filter(p -> p.getOrdem() == ordem).findFirst()
                .orElseThrow(() -> new br.com.fametro.spring.transportadora.exception.custom.RecursoNaoEncontradoException(
                        "Parada " + ordem + " não existe na rota " + codigo + "."));
    }

    /** Baixa uma parada; quando não sobra nenhuma pendente a rota conclui sozinha. */
    public void baixarParada(ParadaRota parada, StatusParada resultado, String motivo, Instant quando, Instant agora) {
        exigir(StatusRota.EM_ANDAMENTO, "baixar paradas de");
        parada.baixar(resultado, motivo, quando);
        if (resultado == StatusParada.ENTREGUE) {
            parada.getCarga().entregar(agora);
        }
        atualizadaEm = agora;
        if (paradas.stream().noneMatch(ParadaRota::pendente)) {
            concluir(agora);
        }
    }

    /** Próxima parada pendente (a de menor ordem). */
    public Optional<ParadaRota> proximaParada() {
        return paradas.stream().filter(ParadaRota::pendente).findFirst();
    }

    private void concluir(Instant agora) {
        status = StatusRota.CONCLUIDA;
        concluidaEm = agora;
        // o que não foi entregue volta com o veículo e fica disponível para outra rota
        paradas.stream().filter(p -> p.getStatus() == StatusParada.NAO_ENTREGUE)
                .forEach(p -> p.getCarga().devolver(agora));
    }

    private void exigir(StatusRota esperado, String acao) {
        if (status != esperado) {
            throw new RegraNegocioException("Não dá para " + acao + " a rota " + codigo + ": ela está " + status
                    + " (precisa estar " + esperado + ").");
        }
    }
}
