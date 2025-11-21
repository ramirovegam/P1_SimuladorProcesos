package p2;
import java.util.*;

public class WorkingSetAlgorithm implements PageReplacementAlgorithm {
    private int frames;
    private int delta;
    private int pageFaults = 0;
    private Integer lastReplacedIndex = null;

    private List<Integer> memory;
    private LinkedList<Integer> recentReferences;

    public WorkingSetAlgorithm(int frames, int delta) {
        this.frames = frames;
        this.delta = delta;
        this.memory = new ArrayList<>();
        this.recentReferences = new LinkedList<>();
    }

    @Override
    public void processPage(Integer page) {
        lastReplacedIndex = null;

        // Actualizar ventana de referencias
        recentReferences.add(page);
        if (recentReferences.size() > delta) {
            recentReferences.removeFirst();
        }

        // Si la página ya está en memoria -> HIT
        if (memory.contains(page)) {
            return;
        }

        // Fallo de página
        pageFaults++;

        // Si hay espacio libre
        if (memory.size() < frames) {
            memory.add(page);
            lastReplacedIndex = memory.size() - 1;
            return;
        }

        // Conjunto de trabajo actual
        Set<Integer> workingSet = new HashSet<>(recentReferences);

        // Buscar página para reemplazar
        for (int i = 0; i < memory.size(); i++) {
            if (!workingSet.contains(memory.get(i))) {
                memory.set(i, page);
                lastReplacedIndex = i;
                return;
            }
        }

        // Si todas están en el conjunto -> usar FIFO (reemplazar la primera)
        memory.set(0, page);
        lastReplacedIndex = 0;
    }

    @Override
    public int getPageFaults() {
        return pageFaults;
    }

    @Override
    public List<Integer> getMemoryState() {
        List<Integer> state = new ArrayList<>(memory);
        while (state.size() < frames) {
            state.add(null);
        }
        return state;
    }

    public Integer getLastReplacedIndex() {
        return lastReplacedIndex;
    }
}
