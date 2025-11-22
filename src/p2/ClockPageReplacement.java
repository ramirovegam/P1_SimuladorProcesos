
package principal;

import java.util.*;

public class ClockPageReplacement {

    public static class Result {
        public final List<String[]> snapshots;
        public final List<String>   labels;
        public final List<Boolean>  faultsPerRow;
        public final List<Integer>  changedIndexPerRow;
        public final List<String>   pagePerRow;
        public final int            frameCount;
        public final int            totalFaults;
        public final int            totalHits;

        // ✅ Nuevos campos para visualización avanzada
        public final List<int[]>    useBitsPerRow;   // useBit por marco en cada paso
        public final List<Integer>  pointerPerRow;   // posición del puntero por paso

        public Result(List<String[]> snapshots,
                      List<String>   labels,
                      List<Boolean>  faultsPerRow,
                      List<Integer>  changedIndexPerRow,
                      List<String>   pagePerRow,
                      int            frameCount,
                      int            totalFaults,
                      int            totalHits,
                      List<int[]>    useBitsPerRow,
                      List<Integer>  pointerPerRow) {
            this.snapshots          = snapshots;
            this.labels             = labels;
            this.faultsPerRow       = faultsPerRow;
            this.changedIndexPerRow = changedIndexPerRow;
            this.pagePerRow         = pagePerRow;
            this.frameCount         = frameCount;
            this.totalFaults        = totalFaults;
            this.totalHits          = totalHits;
            this.useBitsPerRow      = useBitsPerRow;
            this.pointerPerRow      = pointerPerRow;
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

    /** Simulación del algoritmo Clock (Second-Chance en forma de reloj). */
    public Result simulate(List<String> references, int frameCount) {
        if (frameCount <= 0) throw new IllegalArgumentException("El número de marcos debe ser > 0.");

        List<String[]> shots             = new ArrayList<>();
        List<String>   labels            = new ArrayList<>();
        List<Boolean>  faultsPerRow      = new ArrayList<>();
        List<Integer>  changedIndexPerRow= new ArrayList<>();
        List<String>   pagePerRow        = new ArrayList<>();

        // ✅ extras
        List<int[]>    useBitsPerRow     = new ArrayList<>();
        List<Integer>  pointerPerRow     = new ArrayList<>();

        String[] frames = new String[frameCount];
        int[]    useBit = new int[frameCount]; // 0 o 1
        Arrays.fill(useBit, 0);

        int pointer = 0; // puntero del reloj
        int faults = 0, hits = 0;

        // Estado inicial
        shots.add(copy(frames));
        labels.add("Estado inicial:");
        faultsPerRow.add(false);
        changedIndexPerRow.add(-1);
        pagePerRow.add(null);
        useBitsPerRow.add(Arrays.copyOf(useBit, useBit.length));
        pointerPerRow.add(pointer); // puedes usar -1 si no quieres mostrar puntero en inicial

        for (int i = 0; i < references.size(); i++) {
            String page = normalize(references.get(i));
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
                    changedIndex  = empty;
                    // puntero no necesita moverse al colocar en hueco
                } else {
                    // Buscar víctima con segunda oportunidad (reloj)
                    while (true) {
                        if (useBit[pointer] == 0) {
                            frames[pointer] = page;
                            useBit[pointer] = 1;
                            changedIndex    = pointer;
                            // avanza puntero para el futuro
                            pointer = (pointer + 1) % frameCount;
                            break;
                        } else {
                            // segunda oportunidad: pone 0 y avanza
                            useBit[pointer] = 0;
                            pointer = (pointer + 1) % frameCount;
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

            // captura extras
            useBitsPerRow.add(Arrays.copyOf(useBit, useBit.length));
            pointerPerRow.add(pointer); // puntero tras resolver el paso
        }

        return new Result(shots, labels, faultsPerRow, changedIndexPerRow, pagePerRow,
                          frameCount, faults, hits, useBitsPerRow, pointerPerRow);
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
