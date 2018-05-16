import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

/**
 * Frontier is a wrapper for a thread safe queue which keeps track of all the URLs that we need to crawl.
 * It also has a semaphore "permits" responsible for limiting the number of pages crawled.
 */
public class Frontier{
    private ConcurrentLinkedQueue<FrontierElem> frontier;   //thread-safe queue
    private Semaphore permits;                              //limits how many elements of the frontier may be crawled
    
    //TODO: make queue of queue's to track depth. Will need to overhaul this class HARD
    //private int currentDepth;                               //keeps track of the current depth
    
    private final int maxDepth;                             //limits the depth to which we crawl

    public Frontier(Settings settings) {
        frontier = new ConcurrentLinkedQueue<FrontierElem>();
        permits = new Semaphore(settings.getNumPagesToCrawl());
        
        //TODO: uncomment once implemented
        //currentDepth = 0;
        this.maxDepth = settings.getMaxDepth();
    }
    
    
    
    //-----------------------------
    //frontier methods
    //-----------------------------
    
    /**
     * @param e the frontier element to be added
     * @return true on successful insertion, false otherwise.
     * Insertion can fail if the frontierElem is too deep 
     * @see java.util.concurrent.ConcurrentLinkedQueue#add(java.lang.Object)
     */
    public boolean add(FrontierElem e) {
        if(e.getDepth() > maxDepth) return false;
        
        return frontier.add(e);
    }

    /**
     * @param e
     * @return
     * @see java.util.concurrent.ConcurrentLinkedQueue#offer(java.lang.Object)
     */
    public boolean offer(FrontierElem e) {
        if(e.getDepth() > maxDepth) return false;
        
        return frontier.offer(e);
    }

    /**
     * @return
     * @see java.util.concurrent.ConcurrentLinkedQueue#poll()
     */
    public FrontierElem poll() {
        return frontier.poll();
    }

    /**
     * @return
     * @see java.util.concurrent.ConcurrentLinkedQueue#peek()
     */
    public FrontierElem peek() {
        return frontier.peek();
    }

    /**
     * @return the permits
     */
    public Semaphore getPermits() {
        return permits;
    }
    
}