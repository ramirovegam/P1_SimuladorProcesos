
package principal;

import java.util.*;

public class SecondChancePageReplacement {

    public static class Result {
        public final List<String[]> snapshots;
        public final List<String>   labels;
        public final List<Boolean>  faultsPerRow;
        public final List<Integer>  changedIndexPerRow;
        public final List<String>   pagePerRow;
        public final int            frameCount;
        public final int            totalFaults;
        public final int            totalHits;

        // ✅ NUEVO: bits de referencia por paso (tamaño = frameCount)
        public final List<int[]>    refBitsPerRow;

        public Result(List<String[]> snapshots,
                      List<String>   labels,
                      List<Boolean>  faultsPerRow,
                      List<Integer>  changedIndexPerRow,
                      List<String>   pagePerRow,
                      int            frameCount,
                      int            totalFaults,
                      int            totalHits,
                      List<int[]>    refBitsPerRow) {
            this.snapshots          = snapshots;
            this.labels             = labels;
            this.faultsPerRow       = faultsPerRow;
            this.changedIndexPerRow = changedIndexPerRow;
            this.pagePerRow         = pagePerRow;
            this.frameCount         = frameCount;
            this.totalFaults        = totalFaults;
            this.totalHits          = totalHits;
            this.refBitsPerRow      = refBitsPerRow;
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

    public Result simulate(List<String> references, int frameCount) {
        if (frameCount <= 0) throw new IllegalArgumentException("El número de marcos debe ser > 0.");

        List<String[]> shots             = new ArrayList<>();
        List<String>   labels            = new ArrayList<>();
        List<Boolean>  faultsPerRow      = new ArrayList<>();
        List<Integer>  changedIndexPerRow= new ArrayList<>();
        List<String>   pagePerRow        = new ArrayList<>();

        // ✅ captura de bits por paso
        List<int[]>    refBitsPerRow     = new ArrayList<>();

        String[] frames  = new String[frameCount];
        int[]    refBit  = new int[frameCount];
        Arrays.fill(refBit, 0);

        Queue<Integer> fifoQueue = new LinkedList<>(); // posiciones en orden FIFO
        int faults = 0, hits = 0;

        // Estado inicial
        shots.add(copy(frames));
        labels.add("Estado inicial:");
        faultsPerRow.add(false);
        changedIndexPerRow.add(-1);
        pagePerRow.add(null);
        refBitsPerRow.add(Arrays.copyOf(refBit, refBit.length)); // captura inicial

        for (int i = 0; i < references.size(); i++) {
            String page = normalize(references.get(i));
            boolean fault;
            int changedIndex = -1;

            int idx = indexOf(frames, page);
            if (idx >= 0) {
                // Hit: marcar bit de referencia
                hits++;
                fault = false;
                refBit[idx] = 1;
            } else {
                // Fault
                faults++;
                fault = true;
                int empty = firstNull(frames);
                if (empty >= 0) {
                    frames[empty] = page;
                    refBit[empty] = 1;
                    fifoQueue.add(empty);
                    changedIndex = empty;
                } else {
                    // Buscar víctima con segunda oportunidad
                    while (true) {
                        int victim = fifoQueue.poll(); // el más antiguo
                        if (refBit[victim] == 0) {
                            frames[victim] = page;
                            refBit[victim] = 1;
                            fifoQueue.add(victim);
                            changedIndex = victim;
                            break;
                        } else {
                            // Segunda oportunidad: poner bit en 0 y mover al final
                            refBit[victim] = 0;
                            fifoQueue.add(victim);
                        }
                    }
                }
            }

            String label = "Paso " + (i + 1) + ": ENTRA " + page + (fault ? " (Falla)" : " (Hit)");
            shots.add(copy(frames));
            labels.add(label);
            faultsPerRow.add(fault);
            changedIndexPerRow.add(changedIndex);
            pagePerRow.add(page);

            // captura de bits del paso
            refBitsPerRow.add(Arrays.copyOf(refBit, refBit.length));
        }

        return new Result(shots, labels, faultsPerRow, changedIndexPerRow, pagePerRow,
                          frameCount, faults, hits, refBitsPerRow);
    }

    // ===== Helpers =====
    private static String[] copy(String[] src) {
        return Arrays.copyOf(src, src.length);
    }

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
