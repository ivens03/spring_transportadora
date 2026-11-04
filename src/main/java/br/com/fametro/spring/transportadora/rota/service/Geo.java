package br.com.fametro.spring.transportadora.rota.service;

/** Geometria na esfera terrestre (sem dependências: testável isoladamente). */
public final class Geo {

    private static final double RAIO_TERRA_KM = 6371.0088;

    private Geo() {
    }

    public record Ponto(double latitude, double longitude) {
    }

    /** Distância em linha reta (fórmula de Haversine), em km. */
    public static double distanciaKm(Ponto a, Ponto b) {
        double dLat = Math.toRadians(b.latitude() - a.latitude());
        double dLon = Math.toRadians(b.longitude() - a.longitude());
        double h = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(a.latitude())) * Math.cos(Math.toRadians(b.latitude()))
                * Math.pow(Math.sin(dLon / 2), 2);
        return 2 * RAIO_TERRA_KM * Math.asin(Math.min(1, Math.sqrt(h)));
    }
}
