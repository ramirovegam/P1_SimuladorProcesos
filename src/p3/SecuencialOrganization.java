
// p3/SecuencialOrganization.java
package p3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SecuencialOrganization implements FileOrganization {
    private final List<StudentRecord> lista = new ArrayList<>();
    private final Comparator<StudentRecord> byKey = Comparator.comparing(StudentRecord::key);

    @Override public void insert(StudentRecord r) {
        // insert + ordenar; si existe, reemplazar
        int idx = -1;
        for (int i = 0; i < lista.size(); i++) if (lista.get(i).key().equals(r.key())) { idx = i; break; }
        if (idx >= 0) lista.set(idx, r); else lista.add(r);
        Collections.sort(lista, byKey);
    }

    @Override public StudentRecord find(String key) {
        // búsqueda lineal (podrías hacer binaria porque está ordenado)
        for (StudentRecord r : lista) if (r.key().equals(key)) return r;
        return null;
    }

    @Override public String show() {
        StringBuilder sb = new StringBuilder("Secuencial (ordenado por clave):\n");
        for (StudentRecord r : lista) sb.append("  ").append(r).append("\n");
        return sb.toString();
    }
}
