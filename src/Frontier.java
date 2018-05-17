import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Semaphore;

/**
 * Frontier is a wrapper for a thread safe queue which keeps track of all the URLs that we need to crawl.
 * It also has a semaphore "permits" responsible for limiting the number of pages crawled.
 * The Frontier does not accept duplicate elements, where a duplicate is determined by URL
 */
public class Frontier{
    private ConcurrentLinkedQueue<FrontierElem> frontier;   //thread-safe queue
    private ConcurrentMap<String, String> usedUrls;			//used to determine if a URL is or was in the queue
    private Semaphore permits;                              //limits how many elements of the frontier may be crawled
    
    //TODO: make queue of queue's to track depth. Will need to overhaul this class HARD
    //private int currentDepth;                               //keeps track of the current depth
    
    private final int maxDepth;                             //limits the depth to which we crawl

    public Frontier(Settings settings) {
        frontier = new ConcurrentLinkedQueue<FrontierElem>();
        permits = new Semaphore(settings.getNumPagesToCrawl());
        usedUrls = new ConcurrentSkipListMap<>();
        
        //currentDepth = 0; //TODO: uncomment once implemented
        
        this.maxDepth = settings.getMaxDepth();
        
        //initialize the frontier with the seeds
        Scanner seedScanner = null;
        try {
            seedScanner = new Scanner(settings.getSeedPath());
            while(seedScanner.hasNext()){
                String url = seedScanner.next();
                frontier.add(new FrontierElem(url, 0));
                usedUrls.put(url, "");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            seedScanner.close();
        }
    }
    
    /**
     * @return the permits
     */
    public Semaphore getPermits() {
        return permits;
    }
    
    //-----------------------------
    //queue methods
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
}
