package p2;

import java.util.*;

public class FIFOAlgorithm implements PageReplacementAlgorithm {
    private Integer[] memory;
    private int frames;
    private int pageFaults = 0;
    private int nextReplaceIndex = 0; // índice circular
    private Integer lastReplacedIndex = null;

    public FIFOAlgorithm(int frames) {
        this.frames = frames;
        this.memory = new Integer[frames];
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

        // Reemplazo FIFO: usar índice circular
        lastReplacedIndex = nextReplaceIndex;
        memory[nextReplaceIndex] = page;
        nextReplaceIndex = (nextReplaceIndex + 1) % frames;
    }

    @Override
    public int getPageFaults() {
        return pageFaults;
    }

    @Override
    public List<Integer> getMemoryState() {
        return Arrays.asList(Arrays.copyOf(memory, frames)); // copia segura
    }

    public Integer getLastReplacedIndex() {
        return lastReplacedIndex;
    }
}