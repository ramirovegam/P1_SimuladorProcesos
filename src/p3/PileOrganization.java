
// p3/PileOrganization.java
package p3;

import java.util.ArrayList;
import java.util.List;

public class PileOrganization implements FileOrganization {
    private final List<StudentRecord> pile = new ArrayList<>();

    @Override public void insert(StudentRecord r) {
        // Política simple: si ya existe, reemplaza el primero que coincida
        for (int i = 0; i < pile.size(); i++) {
            if (pile.get(i).key().equals(r.key())) { pile.set(i, r); return; }
        }
        pile.add(r);
    }

    @Override public StudentRecord find(String key) {
        for (StudentRecord r : pile) if (r.key().equals(key)) return r;
        return null;
    }

    @Override public String show() {
        StringBuilder sb = new StringBuilder("Pile (append-only):\n");
        for (StudentRecord r : pile) sb.append("  ").append(r).append("\n");
        return sb.toString();
    }
}
