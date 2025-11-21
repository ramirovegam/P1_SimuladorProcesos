package p2;
import java.util.*;

public class PFFAlgorithm implements PageReplacementAlgorithm {
    private List<Integer> memory;
    private int pageFaults = 0;
    private Integer lastReplacedIndex = null;

    private int lowerThreshold;
    private int upperThreshold;
    private int maxFrames;
    private int minFrames;
    private int currentFrames;

    private int referenceCount = 0;
    private int faultCount = 0;

    public PFFAlgorithm(int initialFrames, int minFrames, int maxFrames, double lowerThreshold, double upperThreshold) {
        this.currentFrames = initialFrames;
        this.minFrames = minFrames;
        this.maxFrames = maxFrames;
        this.lowerThreshold = (int)(lowerThreshold * 100); // opcional si quieres manejar como porcentaje
        this.upperThreshold = (int)(upperThreshold * 100);
        this.memory = new ArrayList<>();
    }

    @Override
    public void processPage(Integer page) {
        lastReplacedIndex = null;
        referenceCount++;

        if (memory.contains(page)) {
            adjustFrames();
            return;
        }

        pageFaults++;
        faultCount++;

        if (memory.size() < currentFrames) {
            memory.add(page);
            lastReplacedIndex = memory.size() - 1;
        } else {
            memory.remove(0);
            memory.add(page);
            lastReplacedIndex = currentFrames - 1;
        }

        adjustFrames();
    }

    private void adjustFrames() {
        double pff = (double) faultCount / referenceCount;

        if (pff > (upperThreshold / 100.0) && currentFrames < maxFrames) {
            currentFrames++;
        } else if (pff < (lowerThreshold / 100.0) && currentFrames > minFrames) {
            currentFrames--;
            if (memory.size() > currentFrames) {
                memory.remove(0);
            }
        }
    }

    @Override
    public int getPageFaults() {
        return pageFaults;
    }

    @Override
    public List<Integer> getMemoryState() {
        List<Integer> state = new ArrayList<>(memory);
        while (state.size() < currentFrames) {
            state.add(null);
        }
        return state;
    }

    public Integer getLastReplacedIndex() {
        return lastReplacedIndex;
    }
}