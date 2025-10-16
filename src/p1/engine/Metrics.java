package p1.engine;

public class Metrics {
    public final int espera;
    public final int respuesta;
    public final int turnaround;

    public Metrics(int espera, int respuesta, int turnaround) {
        this.espera = espera;
        this.respuesta = respuesta;
        this.turnaround = turnaround;
    }
}