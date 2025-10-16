package p1.engine;

public class Segment {
    private final String procesoId; // usa "IDLE" para CPU ociosa
    private final int inicio;       // tiempo inclusive
    private final int fin;          // tiempo exclusivo

    public Segment(String procesoId, int inicio, int fin) {
        this.procesoId = procesoId;
        this.inicio = inicio;
        this.fin = fin;
    }
    public String getProcesoId() { return procesoId; }
    public int getInicio() { return inicio; }
    public int getFin() { return fin; }
    public int getDuracion() { return fin - inicio; }
}