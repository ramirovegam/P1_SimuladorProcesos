package p1.engine;

import p1.model.Proceso;
import p1.scheduler.Planificador;
import java.util.List;

public class Simulador {
    private final Planificador planificador;

    public Simulador(Planificador planificador) {
        this.planificador = planificador;
    }
    public SimulationResult run(List<Proceso> procesos) {
        return planificador.simular(procesos);
    }
}