
// p3/SecuencialIndexadoOrganization.java
package p3;

import java.util.*;

public class SecuencialIndexadoOrganization implements FileOrganization {
    private final List<StudentRecord> registros = new ArrayList<>();
    private final Map<String, Integer> indice = new HashMap<>();
    private final Comparator<StudentRecord> byKey = Comparator.comparing(StudentRecord::key);

    @Override public void insert(StudentRecord r) {
        int idx = -1;
        for (int i = 0; i < registros.size(); i++) if (registros.get(i).key().equals(r.key())) { idx = i; break; }
        if (idx >= 0) registros.set(idx, r); else registros.add(r);

        // mantiene orden y reconstruye índice (simple y didáctico)
        registros.sort(byKey);
        indice.clear();
        for (int i = 0; i < registros.size(); i++) indice.put(registros.get(i).key(), i);
    }

    @Override public StudentRecord find(String key) {
        Integer pos = indice.get(key);
        return (pos != null) ? registros.get(pos) : null;
    }

    @Override public String show() {
        return "Secuencial indexado:\nRegistros:\n" +
               registros.stream().map(r -> "  " + r).reduce("", (a,b)->a+b+"\n") +
               "Índice (key->pos): " + indice;
    }
}
