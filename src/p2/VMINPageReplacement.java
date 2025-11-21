
package principal;

import java.util.*;

public class VMINPageReplacement {

    public static class Result {
        public final List<String[]> snapshots;
        public final List<String> labels;
        public final List<Boolean> faultsPerRow;
        public final List<Integer> changedIndexPerRow;
        public final List<String> pagePerRow;
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
     * Simulación VMIN:
     * - Usa una ventana futura (horizon) para decidir la víctima.
     * - Si la página no aparece en la ventana, es candidata inmediata.
     * - Si todas aparecen, se elige la que aparece más tarde en la ventana.
     */
    public Result simulate(List<String> references, int frameCount, int horizon) {
        if (frameCount <= 0) throw new IllegalArgumentException("El número de marcos debe ser > 0.");
        if (horizon <= 0) horizon = 5; // valor por defecto

        List<String[]> shots = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<Boolean> faultsPerRow = new ArrayList<>();
        List<Integer> changedIndexPerRow = new ArrayList<>();
        List<String> pagePerRow = new ArrayList<>();

        String[] frames = new String[frameCount];
        int faults = 0, hits = 0;

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

            if (contains(frames, page)) {
                hits++;
                fault = false;
            } else {
                faults++;
                fault = true;
                int empty = firstNull(frames);
                if (empty >= 0) {
                    frames[empty] = page;
                    changedIndex = empty;
                } else {
                    // Buscar víctima según ventana futura
                    int victim = findVictim(frames, references, i + 1, horizon);
                    frames[victim] = page;
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

    private static int findVictim(String[] frames, List<String> refs, int startIndex, int horizon) {
        int victimIndex = -1;
        int farthest = -1;

        for (int i = 0; i < frames.length; i++) {
            String page = frames[i];
            int nextUse = Integer.MAX_VALUE;
            for (int j = startIndex; j < Math.min(startIndex + horizon, refs.size()); j++) {
                if (page.equals(normalize(refs.get(j)))) {
                    nextUse = j;
                    break;
                }
            }
            if (nextUse == Integer.MAX_VALUE) {
                // No aparece en la ventana → reemplazar inmediatamente
                return i;
            }
            if (nextUse > farthest) {
                farthest = nextUse;
                victimIndex = i;
            }
        }
        return victimIndex;
    }

    private static String[] copy(String[] src) { return Arrays.copyOf(src, src.length); }
    private static boolean contains(String[] frames, String p) {
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

    public static FIFOPageReplacement.Result toFifoResult(Result r) {
        return new FIFOPageReplacement.Result(
                r.snapshots, r.labels, r.faultsPerRow, r.changedIndexPerRow,
                r.pagePerRow, r.frameCount, r.totalFaults, r.totalHits
        );
    }
}
