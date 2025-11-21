
package principal;

import java.util.*;

public class ClockPageReplacement {

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
     * Simulación del algoritmo Clock (Second-Chance):
     * - En hit: useBit[marco] = 1
     * - En fault con hueco: coloca en primer hueco y useBit=1
     * - En fault sin hueco: avanza puntero; si useBit=0 -> reemplaza;
     *   si useBit=1 -> pone 0 y sigue; se detiene al encontrar 0.
     */
    public Result simulate(List<String> references, int frameCount) {
        if (frameCount <= 0) throw new IllegalArgumentException("El número de marcos debe ser > 0.");

        List<String[]> shots = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<Boolean> faultsPerRow = new ArrayList<>();
        List<Integer> changedIndexPerRow = new ArrayList<>();
        List<String> pagePerRow = new ArrayList<>();

        String[] frames = new String[frameCount];
        int[] useBit = new int[frameCount]; // 0 o 1
        Arrays.fill(useBit, 0);

        int pointer = 0;     // puntero del reloj
        int faults = 0, hits = 0;

        // Estado inicial
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

            int idx = indexOf(frames, page);
            if (idx >= 0) {
                // Hit: marca uso
                hits++;
                fault = false;
                useBit[idx] = 1;
            } else {
                // Fault
                faults++;
                fault = true;
                int empty = firstNull(frames);
                if (empty >= 0) {
                    frames[empty] = page;
                    useBit[empty] = 1;
                    changedIndex = empty;
                    // Nota: puntero no necesita moverse por poner en hueco (política común)
                } else {
                    // Busca víctima con "segunda oportunidad"
                    while (true) {
                        if (useBit[pointer] == 0) {
                            // Reemplaza aquí
                            frames[pointer] = page;
                            useBit[pointer] = 1; // recién usado
                            changedIndex = pointer;
                            // Avanza puntero a la siguiente posición para el futuro
                            pointer = (pointer + 1) % frameCount;
                            break;
                        } else {
                            // Da segunda oportunidad (pone 0) y avanza
                            useBit[pointer] = 0;
                            pointer = (pointer + 1) % frameCount;
                        }
                    }
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

    private static String normalize(String p) {
        if (p == null) return null;
        String s = p.trim();
        if (s.isEmpty()) return null;
        return s.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    /** Adaptador para reutilizar SimulationPanel que recibe FIFO.Result. */
    public static FIFOPageReplacement.Result toFifoResult(Result r) {
        return new FIFOPageReplacement.Result(
                r.snapshots, r.labels, r.faultsPerRow, r.changedIndexPerRow,
                r.pagePerRow, r.frameCount, r.totalFaults, r.totalHits
        );
    }
}
