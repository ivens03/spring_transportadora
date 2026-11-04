package br.com.fametro.spring.transportadora.rota.service;

import br.com.fametro.spring.transportadora.rota.config.RotaProperties;
import br.com.fametro.spring.transportadora.rota.dto.response.ParadaResponse;
import br.com.fametro.spring.transportadora.rota.dto.response.PontoResponse;
import br.com.fametro.spring.transportadora.rota.dto.response.PosicaoResponse;
import br.com.fametro.spring.transportadora.rota.dto.response.ProgressoResponse;
import br.com.fametro.spring.transportadora.rota.dto.response.RotaResponse;
import br.com.fametro.spring.transportadora.rota.model.ParadaRota;
import br.com.fametro.spring.transportadora.rota.model.PosicaoRota;
import br.com.fametro.spring.transportadora.rota.model.Rota;
import br.com.fametro.spring.transportadora.rota.model.enums.StatusParada;
import br.com.fametro.spring.transportadora.rota.model.enums.StatusRota;
import br.com.fametro.spring.transportadora.rota.repository.PosicaoRotaRepository;
import br.com.fametro.spring.transportadora.rota.service.Geo.Ponto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Monta a resposta completa da rota, com trajeto, progresso e navegação até a próxima parada. */
@Component
@RequiredArgsConstructor
class RotaMapper {

    private final PosicaoRotaRepository posicaoRotaRepository;
    private final RotaProperties props;

    RotaResponse completa(Rota r) {
        List<ParadaRota> paradas = r.getParadas();

        List<PontoResponse> trajeto = new ArrayList<>(paradas.size() + 2);
        trajeto.add(new PontoResponse("ORIGEM", null, r.getOrigemLatitude(), r.getOrigemLongitude()));
        paradas.forEach(p -> trajeto.add(new PontoResponse("PARADA", p.getOrdem(),
                p.getCarga().getLatitude(), p.getCarga().getLongitude())));
        if (r.isRetornaOrigem()) {
            trajeto.add(new PontoResponse("RETORNO", null, r.getOrigemLatitude(), r.getOrigemLongitude()));
        }

        int entregues = (int) paradas.stream().filter(p -> p.getStatus() == StatusParada.ENTREGUE).count();
        int naoEntregues = (int) paradas.stream().filter(p -> p.getStatus() == StatusParada.NAO_ENTREGUE).count();
        int total = paradas.size();
        int pendentes = total - entregues - naoEntregues;
        ProgressoResponse progresso = new ProgressoResponse(total, entregues, naoEntregues, pendentes,
                total == 0 ? 0 : (int) Math.round((entregues + naoEntregues) * 100.0 / total));

        Optional<PosicaoRota> ultima = r.getStatus() == StatusRota.PLANEJADA
                ? Optional.empty()
                : posicaoRotaRepository.findFirstByRotaIdOrderByRegistradaEmDesc(r.getId());
        Optional<ParadaRota> proxima = r.getStatus().ativa() ? r.proximaParada() : Optional.empty();

        BigDecimal distanciaProxima = null;
        Integer minutosProxima = null;
        if (proxima.isPresent()) {
            Ponto de = ultima.map(p -> ponto(p.getLatitude(), p.getLongitude()))
                    .orElse(ponto(r.getOrigemLatitude(), r.getOrigemLongitude()));
            double km = Geo.distanciaKm(de, ponto(proxima.get().getCarga().getLatitude(),
                    proxima.get().getCarga().getLongitude())) * props.fatorDistanciaViaria();
            distanciaProxima = km(km);
            minutosProxima = (int) Math.ceil(km / props.velocidadeMediaKmH() * 60);
        }

        return new RotaResponse(r.getId(), r.getCodigo(), r.getStatus(),
                r.getVeiculo().getId(), r.getVeiculo().getPlaca(), r.getVeiculo().getModelo(),
                r.getEntregador().getMatricula(), r.getEntregador().getNome(),
                r.getOrigemLatitude(), r.getOrigemLongitude(), r.isRetornaOrigem(),
                r.getDistanciaEstimadaKm(), r.getDuracaoEstimadaMin(), r.getPesoTotalKg(), r.getVolumeTotalM3(),
                paradas.stream().map(ParadaResponse::de).toList(), trajeto, progresso,
                proxima.map(ParadaRota::getOrdem).orElse(null),
                ultima.map(PosicaoResponse::de).orElse(null), distanciaProxima, minutosProxima,
                r.getCriadaPor(), r.getCriadaEm(), r.getIniciadaEm(), r.getConcluidaEm(), r.getCanceladaEm());
    }

    static Ponto ponto(BigDecimal latitude, BigDecimal longitude) {
        return new Ponto(latitude.doubleValue(), longitude.doubleValue());
    }

    static BigDecimal km(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP);
    }
}
