
// p3/HashOrganization.java
package p3;

import java.util.*;

public class HashOrganization implements FileOrganization {
    // simula cubetas (bucketed hashing); maneja colisiones con lista
    private final Map<Integer, List<StudentRecord>> buckets = new HashMap<>();
    private final int M = 97; // tamaño primo para distribuir mejor

    private int h(String key) {
        return Math.floorMod(key.hashCode(), M);
    }

    @Override public void insert(StudentRecord r) {
        int b = h(r.key());
        List<StudentRecord> bucket = buckets.computeIfAbsent(b, k -> new ArrayList<>());
        for (int i = 0; i < bucket.size(); i++) {
            if (bucket.get(i).key().equals(r.key())) { bucket.set(i, r); return; }
        }
        bucket.add(r);
    }

    @Override public StudentRecord find(String key) {
        int b = h(key);
        List<StudentRecord> bucket = buckets.get(b);
        if (bucket == null) return null;
        for (StudentRecord r : bucket) if (r.key().equals(key)) return r;
        return null;
    }

    @Override public String show() {
        StringBuilder sb = new StringBuilder("Hash (M=" + M + "):\n");
        for (Map.Entry<Integer, List<StudentRecord>> e : buckets.entrySet()) {
            sb.append("  [").append(e.getKey()).append("] -> ").append(e.getValue()).append("\n");
        }
        return sb.toString();
    }
}
