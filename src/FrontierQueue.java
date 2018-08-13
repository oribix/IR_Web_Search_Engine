import java.io.IOException;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FrontierQueue implements Frontier{

    private Queue<FrontierElement> queue;
    private DuplicateTracker duplicateTracker;

    FrontierQueue(Settings settings){
        queue = new ConcurrentLinkedQueue<>();
        duplicateTracker = new ConcurrentDuplicateTracker();
        initialize(settings);
    }

    //initialize the frontier
    private void initialize(Settings settings){
        Scanner seedScanner = null;
        try {
            seedScanner = new Scanner(settings.getSeedPath());
            //gets all lines from the document specified in seedPath
            //enters each line into frontier queue
            while(seedScanner.hasNext()){
                String url = seedScanner.next();
                add(new FrontierElement(url, 0));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            assert seedScanner != null;
            seedScanner.close();
        }
    }

    /**
     * Atomically adds Frontier element to the Frontier if it is not a
     * duplicate of a previously added Frontier Element.
     *
     * @param fe Frontier Element to add
     * @return returns true on success, else false.
     */
    @Override
    public boolean add(FrontierElement fe) {
        //fixme: Test for thread safety
        boolean success = false;
        if (duplicateTracker.add(fe)) {
            queue.add(fe);
            success = true;
        }
        return success;
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
