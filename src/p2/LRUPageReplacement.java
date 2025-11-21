package principal;

import java.util.*;

public class LRUPageReplacement {

    /** Resultado de la simulación (mismo esquema que FIFO). */
    public static class Result {
        public final List<String[]> snapshots;          // Fila 0: estado inicial; Fila i: estado tras paso i
        public final List<String> labels;               // "Estado inicial:", "Paso i: entra X"
        public final List<Boolean> faultsPerRow;        // true si la fila i resultó de una falla (fila 0: false)
        public final List<Integer> changedIndexPerRow;  // índice de marco actualizado en la fila i; -1 si no cambió
        public final List<String> pagePerRow;           // página solicitada en la fila i (fila 0: null)
        public final int frameCount;
        public final int totalFaults;
        public final int totalHits;

        public Result(List<String[]> snapshots, List<String> labels,
                      List<Boolean> faultsPerRow, List<Integer> changedIndexPerRow,
                      List<String> pagePerRow, int frameCount,
                      int totalFaults, int totalHits) {
            this.snapshots = snapshots;
            this.labels = labels;
            this.faultsPerRow = faultsPerRow;
            this.changedIndexPerRow = changedIndexPerRow;
            this.pagePerRow = pagePerRow;
            this.frameCount = frameCount;
            this.totalFaults = totalFaults;
            this.totalHits = totalHits;
        }

        public String[] finalFrames() {
            return snapshots.get(snapshots.size() - 1);
        }

        public static String formatFrames(String[] frames) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < frames.length; i++) {
                sb.append(frames[i] == null ? "null" : frames[i]);
                if (i < frames.length - 1) sb.append(" | ");
            }
            return sb.toString();
        }
    }

    /**
     * Simulación LRU:
     * - En hit: actualiza la marca de tiempo (último uso) del marco donde está la página.
     * - En fault con hueco: ocupa el primer marco libre y marca su último uso.
     * - En fault sin hueco: reemplaza el marco con **menor último uso** (el menos reciente).
     */
    public Result simulate(List<String> references, int frameCount) {
        if (frameCount <= 0) throw new IllegalArgumentException("El número de marcos debe ser > 0.");

        List<String[]> shots = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<Boolean> faultsPerRow = new ArrayList<>();
        List<Integer> changedIndexPerRow = new ArrayList<>();
        List<String> pagePerRow = new ArrayList<>();

        String[] frames = new String[frameCount];  // todo null
        int[] lastUsed = new int[frameCount];      // marca de tiempo (paso) del último uso
        Arrays.fill(lastUsed, -1);

        int faults = 0, hits = 0;

        // Fila 0: estado inicial
        shots.add(copy(frames));
        labels.add("Estado inicial:");
        faultsPerRow.add(false);
        changedIndexPerRow.add(-1);
        pagePerRow.add(null);

        for (int i = 0; i < references.size(); i++) {
            int stepTime = i + 1; // tiempo lógico de este paso
            String page = normalize(references.get(i));
            String label = "Paso " + (i + 1) + ": entra " + page;

            int idx = indexOf(frames, page);
            boolean fault;
            int changedIndex = -1;

            if (idx >= 0) {
                // Hit: actualiza "lastUsed" del marco
                hits++;
                fault = false;
                lastUsed[idx] = stepTime;
            } else {
                // Fault
                faults++;
                fault = true;
                int empty = firstNull(frames);
                if (empty >= 0) {
                    frames[empty] = page;
                    lastUsed[empty] = stepTime;
                    changedIndex = empty;
                } else {
                    // Victima: el menos recientemente usado
                    int victim = argMin(lastUsed); // menor lastUsed
                    frames[victim] = page;
                    lastUsed[victim] = stepTime;
                    changedIndex = victim;
                }
            }

            shots.add(copy(frames));
            labels.add(label);
            faultsPerRow.add(fault);
            changedIndexPerRow.add(changedIndex);
            pagePerRow.add(page);
        }

        return new Result(shots, labels, faultsPerRow, changedIndexPerRow, pagePerRow,
                frameCount, faults, hits);
    }

    // ===== Helpers =====

    private static String[] copy(String[] src) { return Arrays.copyOf(src, src.length); }

    private static int indexOf(String[] frames, String page) {
        if (page == null) return -1;
        for (int i = 0; i < frames.length; i++) {
            if (page.equals(frames[i])) return i;
        }
        return -1;
    }

    private static int firstNull(String[] frames) {
        for (int i = 0; i < frames.length; i++) if (frames[i] == null) return i;
        return -1;
    }

    private static int argMin(int[] arr) {
        int idx = 0;
        int min = arr[0];
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] < min) {
                min = arr[i];
                idx = i;
            }
        }
        return idx;
    }

    private static String normalize(String p) {
        if (p == null) return null;
        String s = p.trim();
        if (s.isEmpty()) return null;
        return s.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    // === Adaptador para reusar SimulationPanel (que hoy recibe FIFO.Result) ===
    public static FIFOPageReplacement.Result toFifoResult(Result r) {
        // Copia todos los campos a un FIFO.Result equivalente
        return new FIFOPageReplacement.Result(
                r.snapshots,
                r.labels,
                r.faultsPerRow,
                r.changedIndexPerRow,
                r.pagePerRow,
                r.frameCount,
                r.totalFaults,
                r.totalHits
        );
    }
}
