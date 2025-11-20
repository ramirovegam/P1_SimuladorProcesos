package p2;

import java.util.*;

public class OptimalAlgorithm implements PageReplacementAlgorithm {
    private Integer[] memory;
    private int frames;
    private int pageFaults = 0;
    private Integer lastReplacedIndex = null;
    private List<Integer> futurePages; // lista completa de páginas para predecir

    public OptimalAlgorithm(int frames, List<Integer> futurePages) {
        this.frames = frames;
        this.memory = new Integer[frames];
        this.futurePages = futurePages;
    }

    @Override
    public void processPage(Integer page) {
        lastReplacedIndex = null;

        // Si la página ya está en memoria, no hacemos nada
        for (Integer val : memory) {
            if (val != null && val.equals(page)) {
                return;
            }
        }

        // Fallo de página
        pageFaults++;

        // Si hay espacio libre
        for (int i = 0; i < frames; i++) {
            if (memory[i] == null) {
                memory[i] = page;
                lastReplacedIndex = i; // marcar inserción inicial
                return;
            }
        }

        // Reemplazo Óptimo: buscar la página que se usará más tarde o nunca
        int farthestIndex = -1;
        int maxDistance = -1;

        for (int i = 0; i < frames; i++) {
            Integer currentPage = memory[i];
            int distance = findNextUse(currentPage);
            if (distance > maxDistance) {
                maxDistance = distance;
                farthestIndex = i;
            }
        }

        // Reemplazar la página que se usará más tarde
        memory[farthestIndex] = page;
        lastReplacedIndex = farthestIndex;
    }

    private int findNextUse(Integer page) {
        // Busca la próxima aparición de la página en futurePages
        for (int i = 0; i < futurePages.size(); i++) {
            if (futurePages.get(i).equals(page)) {
                return i; // distancia
            }
        }
        return Integer.MAX_VALUE; // nunca se usará
    }

    @Override
    public int getPageFaults() {
        return pageFaults;
    }

    @Override
    public List<Integer> getMemoryState() {
        return Arrays.asList(Arrays.copyOf(memory, frames));
    }

    public Integer getLastReplacedIndex() {
        return lastReplacedIndex;
    }
}