package p1.engine;

import java.util.List;
import java.util.Map;

public class SimulationResult {
    private final List<Segment> timeline;
    private final Map<String, Metrics> metricsPorProceso;
    private final int tiempoFinal;

    public SimulationResult(List<Segment> timeline, Map<String, Metrics> metricsPorProceso, int tiempoFinal) {
        this.timeline = timeline;
        this.metricsPorProceso = metricsPorProceso;
        this.tiempoFinal = tiempoFinal;
    }
    public List<Segment> getTimeline() { return timeline; }
    public Map<String, Metrics> getMetricsPorProceso() { return metricsPorProceso; }
    public int getTiempoFinal() { return tiempoFinal; }
}