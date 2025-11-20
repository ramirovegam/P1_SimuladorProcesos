package p2;
import java.util.List;

public interface PageReplacementAlgorithm {
    void processPage(Integer page);
    int getPageFaults();
    List<Integer> getMemoryState();
}