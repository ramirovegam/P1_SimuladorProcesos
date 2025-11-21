package p2;
import java.util.*;

public class SecondChanceAlgorithm implements PageReplacementAlgorithm {
    private Queue<PageFrame> queue;
    private int frames;
    private int pageFaults = 0;
    private Integer lastReplacedIndex = null;

    private static class PageFrame {
        int page;
        boolean referenceBit;
        PageFrame(int page) {
            this.page = page;
            this.referenceBit = true;
        }
    }

    public SecondChanceAlgorithm(int frames) {
        this.frames = frames;
        this.queue = new LinkedList<>();
    }

    @Override
    public void processPage(Integer page) {
        lastReplacedIndex = null;

        // Si la página ya está en memoria -> HIT
        for (PageFrame frame : queue) {
            if (frame.page == page) {
                frame.referenceBit = true; // segunda oportunidad
                return;
            }
        }

        // Fallo de página
        pageFaults++;

        // Si hay espacio libre
        if (queue.size() < frames) {
            queue.add(new PageFrame(page));
            lastReplacedIndex = queue.size() - 1;
            return;
        }

        // Reemplazo con segunda oportunidad
        while (true) {
            PageFrame frame = queue.peek();
            if (!frame.referenceBit) {
                queue.poll(); // eliminar
                queue.add(new PageFrame(page));
                lastReplacedIndex = frames - 1; // siempre al final
                break;
            } else {
                frame.referenceBit = false;
                queue.poll();
                queue.add(frame); // mover al final
            }
        }
    }

    @Override
    public int getPageFaults() {
        return pageFaults;
    }

    @Override
    public List<Integer> getMemoryState() {
        List<Integer> state = new ArrayList<>();
        for (PageFrame frame : queue) {
            state.add(frame.page);
        }
        while (state.size() < frames) {
            state.add(null);
        }
        return state;
    }

    public Integer getLastReplacedIndex() {
        return lastReplacedIndex;
    }
}