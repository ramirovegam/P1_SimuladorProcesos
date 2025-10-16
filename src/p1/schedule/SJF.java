package p1.scheduler;

import p1.engine.Metrics;
import p1.engine.Segment;
import p1.engine.SimulationResult;
import p1.model.Proceso;

import java.util.*;

public class SJF implements Planificador {

    @Override
    public SimulationResult simular(List<Proceso> procesos) {
        List<Segment> timeline = new ArrayList<>();
        Map<String, Metrics> metricsMap = new LinkedHashMap<>();

        List<Proceso> pendientes = new ArrayList<>(procesos);
        int tiempo = 0;

        while (!pendientes.isEmpty()) {
            // Buscar procesos que ya llegaron
            List<Proceso> disponibles = new ArrayList<>();
            for (Proceso p : pendientes) {
                if (p.getLlegada() <= tiempo) {
                    disponibles.add(p);
                }
            }

            if (disponibles.isEmpty()) {
                // Avanzar al siguiente proceso más cercano
                int siguienteLlegada = pendientes.stream()
                        .mapToInt(Proceso::getLlegada)
                        .min()
                        .orElse(tiempo + 1);
                timeline.add(new Segment("IDLE", tiempo, siguienteLlegada));
                tiempo = siguienteLlegada;
                continue;
            }

            // Elegir el proceso con menor ráfaga
            Proceso actual = disponibles.stream()
                    .min(Comparator.comparingInt(Proceso::getRafaga))
                    .orElse(disponibles.get(0));

            int inicio = tiempo;
            int fin = inicio + actual.getRafaga();
            timeline.add(new Segment(actual.getId(), inicio, fin));

            int respuesta = inicio - actual.getLlegada();
            int turnaround = fin - actual.getLlegada();
            int espera = turnaround - actual.getRafaga();
            metricsMap.put(actual.getId(), new Metrics(espera, respuesta, turnaround));

            tiempo = fin;
            pendientes.remove(actual);
        }

        return new SimulationResult(timeline, metricsMap, tiempo);
    }
}