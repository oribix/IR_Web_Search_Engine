import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FrontierQueue implements Frontier{

    private Queue<FrontierElement> queue;

    public FrontierQueue(){
        queue = new ConcurrentLinkedQueue<FrontierElement>();
    }

    @Override
    public boolean add(FrontierElement fe) {
        return queue.add(fe);
    }

    @Override
    public boolean offer(FrontierElement fe) {
        return queue.offer(fe);
    }

    @Override
    public FrontierElement remove() {
        return queue.remove();
    }

    @Override
    public FrontierElement poll() {
        return queue.poll();
    }

    @Override
    public FrontierElement element() {
        return queue.element();
    }

    @Override
    public FrontierElement peek() {
        return queue.peek();
    }
}
