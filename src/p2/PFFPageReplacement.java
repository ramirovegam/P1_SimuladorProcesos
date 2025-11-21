
package principal;

import java.util.*;

public class PFFPageReplacement {

    public static class Result {
        public final List<String[]> snapshots;
        public final List<String> labels;
        public final List<Boolean> faultsPerRow;
        public final List<Integer> changedIndexPerRow;
        public final List<String> pagePerRow;
        public final int frameCountFinal;
        public final int totalFaults;
        public final int totalHits;

        public Result(List<String[]> snapshots, List<String> labels,
                      List<Boolean> faultsPerRow, List<Integer> changedIndexPerRow,
                      List<String> pagePerRow, int frameCountFinal,
                      int totalFaults, int totalHits) {
            this.snapshots = snapshots;
            this.labels = labels;
            this.faultsPerRow = faultsPerRow;
            this.changedIndexPerRow = changedIndexPerRow;
            this.pagePerRow = pagePerRow;
            this.frameCountFinal = frameCountFinal;
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

    public Result simulate(List<String> references, int initialFrames, int upperThreshold, int lowerThreshold) {
        if (initialFrames <= 0) throw new IllegalArgumentException("El número inicial de marcos debe ser > 0.");
        if (upperThreshold <= 0 || lowerThreshold <= 0) throw new IllegalArgumentException("Umbrales deben ser > 0.");

        int frameCount = initialFrames;
        List<String[]> shots = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<Boolean> faultsPerRow = new ArrayList<>();
        List<Integer> changedIndexPerRow = new ArrayList<>();
        List<String> pagePerRow = new ArrayList<>();

        String[] frames = new String[frameCount];
        Queue<Integer> fifoQueue = new LinkedList<>();
        int faults = 0, hits = 0;
        int timeSinceLastFault = 0;

        shots.add(copy(frames));
        labels.add("Estado inicial:");
        faultsPerRow.add(false);
        changedIndexPerRow.add(-1);
        pagePerRow.add(null);

        for (int i = 0; i < references.size(); i++) {
            timeSinceLastFault++;
            String page = normalize(references.get(i));
            String label = "Paso " + (i + 1) + ": entra " + page;
            boolean fault;
            int changedIndex = -1;

            int idx = indexOf(frames, page);
            if (idx >= 0) {
                hits++;
                fault = false;
            } else {
                faults++;
                fault = true;
                timeSinceLastFault = 0;

                int empty = firstNull(frames);
                if (empty >= 0) {
                    frames[empty] = page;
                    fifoQueue.add(empty);
                    changedIndex = empty;
                } else {
                    int victim = fifoQueue.poll();
                    frames[victim] = page;
                    fifoQueue.add(victim);
                    changedIndex = victim;
                }
            }

            // Ajuste dinámico de marcos
            if (timeSinceLastFault < upperThreshold) {
                // Alta frecuencia de fallas → aumentar marcos
                frameCount++;
                frames = Arrays.copyOf(frames, frameCount);
            } else if (timeSinceLastFault > lowerThreshold && frameCount > 1) {
                // Baja frecuencia → reducir marcos
                frameCount--;
                frames = Arrays.copyOf(frames, frameCount);
                // Ajustar la cola si se reduce
                while (fifoQueue.size() > frameCount) fifoQueue.poll();
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
    private static String normalize(String p) {
        if (p == null) return null;
        String s = p.trim();
        if (s.isEmpty()) return null;
        return s.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    public static FIFOPageReplacement.Result toFifoResult(Result r) {
        return new FIFOPageReplacement.Result(
                r.snapshots, r.labels, r.faultsPerRow, r.changedIndexPerRow,
                r.pagePerRow, r.frameCountFinal, r.totalFaults, r.totalHits
        );
    }
}
