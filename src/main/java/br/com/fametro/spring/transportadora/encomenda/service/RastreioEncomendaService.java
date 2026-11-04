package br.com.fametro.spring.transportadora.encomenda.service;

import br.com.fametro.spring.transportadora.carga.model.Carga;
import br.com.fametro.spring.transportadora.carga.model.enums.StatusCarga;
import br.com.fametro.spring.transportadora.encomenda.dto.response.LocalizacaoResponse;
import br.com.fametro.spring.transportadora.encomenda.dto.response.RastreioResponse;
import br.com.fametro.spring.transportadora.rota.config.RotaProperties;
import br.com.fametro.spring.transportadora.rota.model.ParadaRota;
import br.com.fametro.spring.transportadora.rota.model.PosicaoRota;
import br.com.fametro.spring.transportadora.rota.model.Rota;
import br.com.fametro.spring.transportadora.rota.model.enums.StatusParada;
import br.com.fametro.spring.transportadora.rota.model.enums.StatusRota;
import br.com.fametro.spring.transportadora.rota.repository.ParadaRotaRepository;
import br.com.fametro.spring.transportadora.rota.repository.PosicaoRotaRepository;
import br.com.fametro.spring.transportadora.rota.service.Geo;
import br.com.fametro.spring.transportadora.rota.service.Geo.Ponto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Calcula onde a encomenda está e quando deve chegar. Usado pelo portal (GET /encomendas/{codigo}) e pelo
 * conteúdo dos webhooks — os dois mostram exatamente a mesma coisa.
 * <p>
 * Previsão = da última posição do veículo, passando pelas entregas pendentes ANTES desta (na ordem da rota),
 * até o destino: linha reta × fator viário ÷ velocidade média + minutos por entrega anterior
 * (mesmas premissas da roteirização, ver {@link RotaProperties}).
 */
@Service
@RequiredArgsConstructor
public class RastreioEncomendaService {

    private static final Set<StatusCarga> NUMA_ROTA = Set.of(StatusCarga.ROTEIRIZADA, StatusCarga.EM_TRANSITO, StatusCarga.ENTREGUE);

    private final ParadaRotaRepository paradaRotaRepository;
    private final PosicaoRotaRepository posicaoRotaRepository;
    private final RotaProperties props;
    private final Clock clock;

    /** Vazio quando a encomenda não está em rota (aguardando, cancelada). */
    @Transactional(readOnly = true)
    public Optional<RastreioResponse> rastrear(Carga carga) {
        if (!NUMA_ROTA.contains(carga.getStatus())) {
            return Optional.empty();
        }
        return paradaRotaRepository.maisRecentesDaCarga(carga.getId(), Limit.of(1)).stream().findFirst()
                .map(parada -> rastrear(parada, ultimaPosicao(parada.getRota())));
    }

    /** Variante para quem já tem a rota e a última posição em mãos (webhook de localização: uma consulta por rota). */
    public RastreioResponse rastrear(ParadaRota parada, Optional<PosicaoRota> ultima) {
        Rota rota = parada.getRota();
        boolean aCaminho = rota.getStatus() == StatusRota.EM_ANDAMENTO && parada.pendente();
        boolean planejada = rota.getStatus() == StatusRota.PLANEJADA && parada.pendente();

        Integer antes = null;
        BigDecimal distancia = null;
        Integer minutos = null;
        Instant chegada = null;
        LocalizacaoResponse localizacao = aCaminho ? ultima.map(LocalizacaoResponse::de).orElse(null) : null;

        if (aCaminho || planejada) {
            List<ParadaRota> anteriores = rota.getParadas().stream()
                    .filter(p -> p.pendente() && p.getOrdem() < parada.getOrdem()).toList();
            Ponto de = aCaminho && ultima.isPresent()
                    ? ponto(ultima.get().getLatitude(), ultima.get().getLongitude())
                    : ponto(rota.getOrigemLatitude(), rota.getOrigemLongitude());
            double km = 0;
            for (ParadaRota p : anteriores) {
                Ponto proximo = ponto(p.getCarga().getLatitude(), p.getCarga().getLongitude());
                km += Geo.distanciaKm(de, proximo);
                de = proximo;
            }
            km += Geo.distanciaKm(de, ponto(parada.getCarga().getLatitude(), parada.getCarga().getLongitude()));
            km *= props.fatorDistanciaViaria();

            antes = anteriores.size();
            distancia = BigDecimal.valueOf(km).setScale(2, RoundingMode.HALF_UP);
            minutos = (int) Math.ceil(km / props.velocidadeMediaKmH() * 60) + antes * props.minutosPorParada();
            // antes de sair não dá para dizer a hora: depende de quando o entregador iniciar
            chegada = aCaminho ? clock.instant().plusSeconds(minutos * 60L) : null;
        }

        Instant entregueEm = parada.getStatus() == StatusParada.ENTREGUE ? parada.getFinalizadaEm() : null;
        String nome = rota.getEntregador().getNome().trim();
        String primeiroNome = nome.contains(" ") ? nome.substring(0, nome.indexOf(' ')) : nome;
        return new RastreioResponse(rota.getCodigo(), rota.getStatus(), primeiroNome,
                rota.getVeiculo().getModelo(), rota.getVeiculo().getPlaca(), parada.getOrdem(), antes, localizacao,
                distancia, minutos, chegada, entregueEm);
    }

    public Optional<PosicaoRota> ultimaPosicao(Rota rota) {
        return rota.getStatus() == StatusRota.PLANEJADA
                ? Optional.empty()
                : posicaoRotaRepository.findFirstByRotaIdOrderByRegistradaEmDesc(rota.getId());
    }

    private static Ponto ponto(BigDecimal latitude, BigDecimal longitude) {
        return new Ponto(latitude.doubleValue(), longitude.doubleValue());
    }
}
