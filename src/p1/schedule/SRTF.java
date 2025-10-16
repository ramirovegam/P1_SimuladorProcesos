package p1.scheduler;

import p1.engine.Metrics;
import p1.engine.Segment;
import p1.engine.SimulationResult;
import p1.model.Proceso;

import java.util.*;

public class SRTF implements Planificador {
    @Override
    public SimulationResult simular(List<Proceso> procesos) {
        List<Segment> timeline = new ArrayList<>();
        Map<String, Metrics> metricsMap = new LinkedHashMap<>();
        Map<String, Integer> remainingTime = new HashMap<>();
        Map<String, Integer> startTimes = new HashMap<>();
        Map<String, Integer> finishTimes = new HashMap<>();

        for (Proceso p : procesos) {
            remainingTime.put(p.getId(), p.getRafaga());
        }

        int tiempo = 0;
        Proceso actual = null;
        int inicio = -1;

        while (!remainingTime.isEmpty()) {
            List<Proceso> disponibles = new ArrayList<>();
            for (Proceso p : procesos) {
                if (p.getLlegada() <= tiempo && remainingTime.containsKey(p.getId())) {
                    disponibles.add(p);
                }
            }

            if (disponibles.isEmpty()) {
                tiempo++;
                continue;
            }

            Proceso siguiente = disponibles.stream()
                    .min(Comparator.comparingInt(p -> remainingTime.get(p.getId())))
                    .orElse(disponibles.get(0));

            if (actual == null || !actual.getId().equals(siguiente.getId())) {
                if (actual != null && inicio != -1) {
                    timeline.add(new Segment(actual.getId(), inicio, tiempo));
                }
                actual = siguiente;
                inicio = tiempo;
                if (!startTimes.containsKey(actual.getId())) {
                    startTimes.put(actual.getId(), tiempo);
                }
            }

            remainingTime.put(actual.getId(), remainingTime.get(actual.getId()) - 1);
            tiempo++;

            if (remainingTime.get(actual.getId()) == 0) {
                timeline.add(new Segment(actual.getId(), inicio, tiempo));
                finishTimes.put(actual.getId(), tiempo);
                remainingTime.remove(actual.getId());
                actual = null;
                inicio = -1;
            }
        }

        for (Proceso p : procesos) {
            int start = startTimes.get(p.getId());
            int finish = finishTimes.get(p.getId());
            int respuesta = start - p.getLlegada();
            int turnaround = finish - p.getLlegada();
            int espera = turnaround - p.getRafaga();
            metricsMap.put(p.getId(), new Metrics(espera, respuesta, turnaround));
        }

        return new SimulationResult(timeline, metricsMap, tiempo);
    }
}