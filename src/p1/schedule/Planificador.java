package p1.scheduler;

import p1.engine.SimulationResult;
import p1.model.Proceso;
import java.util.List;

public interface Planificador {
    /**
     * Recibe la lista de procesos y devuelve un resultado completo (timeline + métricas).
     * Implementaremos primero FCFS.
     */
    SimulationResult simular(List<Proceso> procesos);
}