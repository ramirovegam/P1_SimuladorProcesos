package p1.model;

public class Proceso {
    private final String id;
    private final int llegada;
    private final int rafaga;

    public Proceso(String id, int llegada, int rafaga) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("ID vacío");
        if (llegada < 0) throw new IllegalArgumentException("Llegada no puede ser negativa");
        if (rafaga <= 0) throw new IllegalArgumentException("Ráfaga debe ser > 0");
        this.id = id;
        this.llegada = llegada;
        this.rafaga = rafaga;
    }
    public String getId() { return id; }
    public int getLlegada() { return llegada; }
    public int getRafaga() { return rafaga; }
}