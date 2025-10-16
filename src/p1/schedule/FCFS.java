package p1.scheduler;

import p1.engine.Metrics;
import p1.engine.Segment;
import p1.engine.SimulationResult;
import p1.model.Proceso;

import java.util.*;

public class FCFS implements Planificador {

    @Override
    public SimulationResult simular(List<Proceso> procesos) {
        // Copia y orden por llegada (si igual, respetar orden de aparición)
        List<Proceso> cola = new ArrayList<>(procesos);
        cola.sort(Comparator.comparingInt(Proceso::getLlegada));

        List<Segment> timeline = new ArrayList<>();
        Map<String, Integer> startTimes = new HashMap<>();
        Map<String, Integer> finishTimes = new HashMap<>();

        int tiempo = 0;

        for (Proceso p : cola) {
            // Si la CPU está ociosa antes de que llegue el siguiente proceso, agrega segmento IDLE
            if (tiempo < p.getLlegada()) {
                timeline.add(new Segment("IDLE", tiempo, p.getLlegada()));
                tiempo = p.getLlegada();
            }
            // Comienza a ejecutar el proceso inmediatamente a su llegada o cuando la CPU esté libre
            int inicio = tiempo;
            int fin = inicio + p.getRafaga();

            timeline.add(new Segment(p.getId(), inicio, fin));
            startTimes.put(p.getId(), inicio);
            finishTimes.put(p.getId(), fin);

            tiempo = fin; // avanza el reloj
        }

        // Métricas por proceso
        Map<String, Metrics> m = new LinkedHashMap<>();
        for (Proceso p : cola) {
            int inicio = startTimes.get(p.getId());
            int fin = finishTimes.get(p.getId());
            int respuesta  = inicio - p.getLlegada();
            int turnaround = fin - p.getLlegada();
            int espera     = turnaround - p.getRafaga();
            m.put(p.getId(), new Metrics(espera, respuesta, turnaround));
        }

        return new SimulationResult(timeline, m, tiempo);
    }
}