package p2;
import java.util.List;

public class SimulationStep {
    private Integer page;
    private List<Integer> memoryState;
    private boolean pageFault;
    private Integer replacedIndex; // nuevo campo

    public SimulationStep(Integer page, List<Integer> memoryState, boolean pageFault, Integer replacedIndex) {
        this.page = page;
        this.memoryState = memoryState;
        this.pageFault = pageFault;
        this.replacedIndex = replacedIndex;
    }

    public Integer getPage() { return page; }
    public List<Integer> getMemoryState() { return memoryState; }
    public boolean isPageFault() { return pageFault; }
    public Integer getReplacedIndex() { return replacedIndex; }
}
