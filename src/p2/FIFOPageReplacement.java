package principal;

import java.util.*;

public class FIFOPageReplacement {

    /** Resultado de la simulación con métricas y metadatos por paso/fila. */
    public static class Result {
        public final List<String[]> snapshots;       // Fila 0: estado inicial; Fila i: estado tras paso i
        public final List<String> labels;            // "Estado inicial:", "Paso i: entra X"
        public final List<Boolean> faultsPerRow;     // true si la fila i resultó de una falla (fila 0: false)
        public final List<Integer> changedIndexPerRow; // índice de marco actualizado en la fila i, -1 si no cambió
        public final List<String> pagePerRow;        // página del paso i (fila 0: null)
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

    /** Simula FIFO con páginas (letras) y calcula métricas y metadatos por paso. */
    public Result simulate(List<String> references, int frameCount) {
        if (frameCount <= 0) throw new IllegalArgumentException("El número de marcos debe ser > 0.");

        List<String[]> shots = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<Boolean> faultsPerRow = new ArrayList<>();
        List<Integer> changedIndexPerRow = new ArrayList<>();
        List<String> pagePerRow = new ArrayList<>();

        String[] frames = new String[frameCount];      // todo null al inicio
        Deque<Integer> fifoQueue = new ArrayDeque<>(); // orden de marcos ocupados (índices)
        int faults = 0, hits = 0;

        // Fila 0: estado inicial (sin página)
        shots.add(copy(frames));
        labels.add("Estado inicial:");
        faultsPerRow.add(false);
        changedIndexPerRow.add(-1);
        pagePerRow.add(null);

        for (int i = 0; i < references.size(); i++) {
            String page = normalize(references.get(i));
            String label = "Paso " + (i + 1) + ": entra " + page;

            boolean fault;
            int changedIndex = -1;

            if (!contains(frames, page)) {
                fault = true;
                faults++;
                int empty = firstNull(frames);
                if (empty >= 0) {
                    frames[empty] = page;
                    fifoQueue.addLast(empty);
                    changedIndex = empty; // marco llenado
                } else {
                    int victim = fifoQueue.removeFirst();
                    frames[victim] = page;
                    fifoQueue.addLast(victim);
                    changedIndex = victim; // marco reemplazado
                }
            } else {
                fault = false;
                hits++;
                // En FIFO, el orden no cambia en un hit.
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

    private static boolean contains(String[] frames, String p) {
        if (p == null) return false;
        for (String f : frames) if (p.equals(f)) return true;
        return false;
    }

    private static int firstNull(String[] frames) {
        for (int i = 0; i < frames.length; i++) if (frames[i] == null) return i;
        return -1;
    }

    private static String normalize(String p) {
        if (p == null) return null;
        String s = p.trim();
        if (s.isEmpty()) return null;
        return s.substring(0, 1).toUpperCase(Locale.ROOT);
    }
}
