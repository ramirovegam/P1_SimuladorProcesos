
package p1.schedule;

import p1.engine.Metrics;
import p1.engine.Segment;
import p1.engine.SimulationResult;
import p1.model.Proceso;

import java.util.*;
import p1.scheduler.Planificador;

public class RR implements Planificador {

    private final int quantum;

    public RR(int quantum) {
        this.quantum = quantum;
    }

    @Override
    public SimulationResult simular(List<Proceso> procesos) {
        List<Segment> timeline = new ArrayList<>();
        Map<String, Metrics> metricsMap = new LinkedHashMap<>();

        Queue<Proceso> cola = new LinkedList<>();
        Map<String, Integer> restante = new HashMap<>();
        Map<String, Integer> llegada = new HashMap<>();
        Map<String, Integer> inicio = new HashMap<>();

        procesos.sort(Comparator.comparingInt(Proceso::getLlegada));
        for (Proceso p : procesos) {
            restante.put(p.getId(), p.getRafaga());
            llegada.put(p.getId(), p.getLlegada());
        }

        int tiempo = 0;
        int index = 0;

        while (!cola.isEmpty() || index < procesos.size()) {
            while (index < procesos.size() && procesos.get(index).getLlegada() <= tiempo) {
                cola.add(procesos.get(index));
                index++;
            }

            if (cola.isEmpty()) {
                timeline.add(new Segment("IDLE", tiempo, tiempo + 1));
                tiempo++;
                continue;
            }

            Proceso actual = cola.poll();
            String pid = actual.getId();
            int restanteActual = restante.get(pid);
            int duracion = Math.min(quantum, restanteActual);

            timeline.add(new Segment(pid, tiempo, tiempo + duracion));

            if (!inicio.containsKey(pid)) {
                inicio.put(pid, tiempo);
            }

            tiempo += duracion;
            restante.put(pid, restanteActual - duracion);

            // Agregar nuevos procesos que llegaron durante la ejecución
            while (index < procesos.size() && procesos.get(index).getLlegada() <= tiempo) {
                cola.add(procesos.get(index));
                index++;
            }

            // Si el proceso no ha terminado, vuelve a la cola
            if (restante.get(pid) > 0) {
                cola.add(actual);
            } else {
                int llegadaP = llegada.get(pid);
                int inicioP = inicio.get(pid);
                int turnaround = tiempo - llegadaP;
                int espera = turnaround - actual.getRafaga();
                int respuesta = inicioP - llegadaP;
                metricsMap.put(pid, new Metrics(espera, respuesta, turnaround));
            }
        }

        return new SimulationResult(timeline, metricsMap, tiempo);
    }
}
