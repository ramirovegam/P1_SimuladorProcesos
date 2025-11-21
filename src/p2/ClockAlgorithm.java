package p2;
import java.util.*;

public class ClockAlgorithm implements PageReplacementAlgorithm {
    private Integer[] memory;
    private int[] referenceBits;
    private int frames;
    private int pageFaults = 0;
    private int pointer = 0;
    private Integer lastReplacedIndex = null;

    public ClockAlgorithm(int frames) {
        this.frames = frames;
        this.memory = new Integer[frames];
        this.referenceBits = new int[frames];
    }

    @Override
    public void processPage(Integer page) {
        lastReplacedIndex = null;

        // Si la página ya está en memoria -> HIT
        for (int i = 0; i < frames; i++) {
            if (memory[i] != null && memory[i].equals(page)) {
                referenceBits[i] = 1; // marcar referencia
                return;
            }
        }

        // Fallo de página
        pageFaults++;

        // Si hay espacio libre
        for (int i = 0; i < frames; i++) {
            if (memory[i] == null) {
                memory[i] = page;
                referenceBits[i] = 1;
                lastReplacedIndex = i;
                return;
            }
        }

        // Reemplazo usando Clock
        while (true) {
            if (referenceBits[pointer] == 0) {
                memory[pointer] = page;
                referenceBits[pointer] = 1;
                lastReplacedIndex = pointer;
                pointer = (pointer + 1) % frames;
                break;
            } else {
                referenceBits[pointer] = 0;
                pointer = (pointer + 1) % frames;
            }
        }
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