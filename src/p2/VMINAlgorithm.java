package p2;
import java.util.*;

public class VMINAlgorithm implements PageReplacementAlgorithm {
    private Integer[] memory;
    private int frames;
    private int pageFaults = 0;
    private Integer lastReplacedIndex = null;
    private List<Integer> futurePages;
    private int currentIndex = 0;
    private int windowSize;

    public VMINAlgorithm(int frames, List<Integer> futurePages, int windowSize) {
        this.frames = frames;
        this.memory = new Integer[frames];
        this.futurePages = futurePages;
        this.windowSize = windowSize;
    }

    @Override
    public void processPage(Integer page) {
        lastReplacedIndex = null;

        // Si la página ya está en memoria -> HIT
        for (Integer val : memory) {
            if (val != null && val.equals(page)) {
                currentIndex++;
                return;
            }
        }

        // Fallo de página
        pageFaults++;

        // Si hay espacio libre
        for (int i = 0; i < frames; i++) {
            if (memory[i] == null) {
                memory[i] = page;
                lastReplacedIndex = i;
                currentIndex++;
                return;
            }
        }

        // Reemplazo VMIN
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

        memory[farthestIndex] = page;
        lastReplacedIndex = farthestIndex;
        currentIndex++;
    }

    private int findNextUse(Integer page) {
        int limit = Math.min(currentIndex + windowSize, futurePages.size());
        for (int i = currentIndex + 1; i < limit; i++) {
            if (futurePages.get(i).equals(page)) {
                return i - currentIndex;
            }
        }
        return Integer.MAX_VALUE; // no se usará en la ventana
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