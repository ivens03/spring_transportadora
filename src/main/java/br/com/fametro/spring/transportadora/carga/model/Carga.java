package br.com.fametro.spring.transportadora.carga.model;

import br.com.fametro.spring.transportadora.carga.model.enums.StatusCarga;
import br.com.fametro.spring.transportadora.usuario.model.Usuario;
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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/** Carga (volume/encomenda) a ser entregue num endereço com coordenadas. */
@Entity
@Table(name = "carga")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Carga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** CRG-000001. */
    @Column(nullable = false, unique = true, updatable = false)
    private String codigo;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false)
    private BigDecimal pesoKg;

    @Column(nullable = false)
    private BigDecimal volumeM3;

    @Column(nullable = false)
    private String destinatarioNome;

    private String destinatarioTelefone;

    @Column(nullable = false)
    private String endereco;

    @Column(nullable = false)
    private String bairro;

    @Column(nullable = false)
    private String cidade;

    @Column(nullable = false)
    private String uf;

    private String cep;

    @Column(nullable = false)
    private BigDecimal latitude;

    @Column(nullable = false)
    private BigDecimal longitude;

    /** Dono da encomenda (perfil CLIENTE): acompanha pelo portal e recebe os webhooks. Opcional. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Usuario cliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusCarga status;

    /** Quantas vezes saiu para entrega e voltou sem entregar. */
    @Column(nullable = false)
    private int tentativasEntrega;

    @Column(nullable = false, updatable = false)
    private String cadastradaPor;

    @Column(nullable = false, updatable = false)
    private Instant criadaEm;

    @Column(nullable = false)
    private Instant atualizadaEm;

    public static Carga nova(String codigo, String descricao, BigDecimal pesoKg, BigDecimal volumeM3,
                             String destinatarioNome, String destinatarioTelefone, String endereco, String bairro,
                             String cidade, String uf, String cep, BigDecimal latitude, BigDecimal longitude,
                             Usuario cliente, String cadastradaPor, Instant agora) {
        Carga c = new Carga();
        c.codigo = codigo;
        c.descricao = descricao;
        c.pesoKg = pesoKg;
        c.volumeM3 = volumeM3;
        c.destinatarioNome = destinatarioNome;
        c.destinatarioTelefone = destinatarioTelefone;
        c.endereco = endereco;
        c.bairro = bairro;
        c.cidade = cidade;
        c.uf = uf;
        c.cep = cep;
        c.latitude = latitude;
        c.longitude = longitude;
        c.cliente = cliente;
        c.status = StatusCarga.AGUARDANDO_ROTA;
        c.cadastradaPor = cadastradaPor;
        c.criadaEm = agora;
        c.atualizadaEm = agora;
        return c;
    }

    public boolean disponivelParaRota() {
        return status == StatusCarga.AGUARDANDO_ROTA;
    }

    public void alterarDescricao(String v, Instant agora) { descricao = v; tocar(agora); }
    public void alterarPesoKg(BigDecimal v, Instant agora) { pesoKg = v; tocar(agora); }
    public void alterarVolumeM3(BigDecimal v, Instant agora) { volumeM3 = v; tocar(agora); }
    public void alterarDestinatarioNome(String v, Instant agora) { destinatarioNome = v; tocar(agora); }
    public void alterarDestinatarioTelefone(String v, Instant agora) { destinatarioTelefone = v; tocar(agora); }
    public void alterarEndereco(String endereco, String bairro, String cidade, String uf, String cep,
                                BigDecimal latitude, BigDecimal longitude, Instant agora) {
        this.endereco = endereco;
        this.bairro = bairro;
        this.cidade = cidade;
        this.uf = uf;
        this.cep = cep;
        this.latitude = latitude;
        this.longitude = longitude;
        tocar(agora);
    }

    /** Troca (ou remove, com nulo) o cliente dono da encomenda. */
    public void vincularCliente(Usuario cliente, Instant agora) { this.cliente = cliente; tocar(agora); }

    public void cancelar(Instant agora) { status = StatusCarga.CANCELADA; tocar(agora); }
    public void roteirizar(Instant agora) { status = StatusCarga.ROTEIRIZADA; tocar(agora); }
    public void embarcar(Instant agora) { status = StatusCarga.EM_TRANSITO; tocar(agora); }
    public void entregar(Instant agora) { status = StatusCarga.ENTREGUE; tocar(agora); }

    /** Rota cancelada antes de sair: a carga volta para a fila sem contar tentativa. */
    public void liberar(Instant agora) { status = StatusCarga.AGUARDANDO_ROTA; tocar(agora); }

    /** Saiu e voltou sem entregar: volta para a fila e conta a tentativa. */
    public void devolver(Instant agora) {
        status = StatusCarga.AGUARDANDO_ROTA;
        tentativasEntrega++;
        tocar(agora);
    }

    private void tocar(Instant agora) {
        atualizadaEm = agora;
    }
}
