package p2;

import java.util.*;

public class LRUAlgorithm implements PageReplacementAlgorithm {
    private LinkedList<Integer> memory;
    private int frames;
    private int pageFaults = 0;
    private Integer lastReplacedIndex = null;

    public LRUAlgorithm(int frames) {
        this.frames = frames;
        this.memory = new LinkedList<>();
    }

    @Override
    public void processPage(Integer page) {
        lastReplacedIndex = null;

        // Si la página ya está en memoria -> HIT
        if (memory.contains(page)) {
            // Mover al final (más reciente)
            memory.remove(page);
            memory.addLast(page);
            return;
        }

        // Fallo de página
        pageFaults++;

        if (memory.size() < frames) {
            memory.addLast(page);
            lastReplacedIndex = memory.size() - 1; // posición donde se insertó
        } else {
            // Eliminar el menos reciente (primero)
            memory.removeFirst();
            memory.addLast(page);
            lastReplacedIndex = frames - 1; // siempre el último
        }
    }

    @Override
    public int getPageFaults() {
        return pageFaults;
    }

    @Override
    public List<Integer> getMemoryState() {
        // Devuelve lista con tamaño fijo (frames), usando "-" para vacíos
        List<Integer> state = new ArrayList<>(frames);
        for (int i = 0; i < frames; i++) {
            if (i < memory.size()) {
                state.add(memory.get(i));
            } else {
                state.add(null);
            }
        }
        return state;
    }

    public Integer getLastReplacedIndex() {
        return lastReplacedIndex;
    }
    
    
}