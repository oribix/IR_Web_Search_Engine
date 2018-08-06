import java.io.File;
import java.io.IOException;
import java.lang.reflect.GenericArrayType;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;

public class CrawlerThread extends Thread{
    
    private FileManager fileManager;
    private Settings settings;
    private int threadid;
    
    //shared thread variables
    private Frontier frontier;              //see Frontier for information
    private Semaphore pagesLeft;            //number of pages left to crawl
    private int waitCount;                  //each thread has own WaitCount, goes up every time frontier's empty
    
    //TODO:somehow make this prettier
    private static Object validLock;        //lock used with url_doc_map
    static {
        validLock = new Object();
    }
    
    //TODO: Phase these out or rework
    private AtomicIntegerArray levelLimits;
    private String levelLimitChecker;
    private AtomicInteger pagesCrawled;     //number of pages successfully crawled so far
                                            //TODO: Maybe use FileManager.docCount?
    
    //CrawlerThread constructor
    CrawlerThread(Frontier frontier,
                  FileManager fileManager,
                  Settings settings,
                  AtomicInteger pagesCrawled,
                  int threadid,
                  AtomicIntegerArray ll,
                  String llc){
        this.frontier = frontier;
        this.fileManager = fileManager;
        this.settings = settings;
        this.pagesCrawled = pagesCrawled;
        this.threadid = threadid;
        
        //TODO: Phase these out
        this.pagesLeft = frontier.getPermits();
        this.levelLimits = ll;
        this.levelLimitChecker = llc;
        
    }
    
    //This handles the actions of the thread
    @Override
    public void run() {
        //While we havn't crawled enough pages or crawled within the specified depth
        while((pagesCrawled.get() < settings.getNumPagesToCrawl()) && !(Objects.equals(levelLimits.toString(), levelLimitChecker))){
            if(pagesLeft.tryAcquire()){
                FrontierElem frontierElem = frontier.poll(); //get next URL in queue

                if(frontierElem != null) {
                    //frontier queue wasn't empty, so WaitCount resets
                    waitCount = 0;
                    //LevelLimits array shows this thread isn't waiting 
                    levelLimits.compareAndSet(threadid, 1, 0);

                    //if the URL doesn't have a protocol, attempts to fix it
                    String url = frontierElem.getUrl();
                    if(!url.startsWith("http://") && !url.startsWith("https://")){
                        System.out.println("ERROR: URL HAS NO PROTOCOL! Attemping recovery by prepending protocol");
                        frontierElem.setUrl("http://" + url);
                    }

                    //downloads the URL
                    if(pagesCrawled.get() < settings.getNumPagesToCrawl()){
                        if(downloadFile(frontierElem)){
                            //keeps track of how many pages we have crawled
                            int p = pagesCrawled.incrementAndGet();
                            if(p % 100 == 0)System.out.println("Pages Crawled: " + p);
                        }
                        else pagesLeft.release(); //downloadFile failed. Release permit
                    }
                }
                else {
                    waitCount++;
                    pagesLeft.release();//URL invalid. Release permit
                }
            }
            if (waitCount > 1000) {
                //if the current thread is waiting, set the int in the LevelLimits array to 1
                //this says that this thread isn't finding any pages in the queue
                levelLimits.set(threadid, 1);
            }
        }
        return;
    }
    
    FrontierElem[] crawlUrl(FrontierElem frontierElem) {
        
        return null;
    }
    
    //downloads the page at the specified URL's location
    //returns true on success
    private boolean downloadFile(FrontierElem frontierElem){
        
        //save the web page in a file
        String fileName = fileManager.saveAsFile(htmlContent);
        if(fileName == null) {
            System.out.println("error saving document. url: " + url);
            return false;
        }
        
        //checks if a URL is valid. Records it if it is to prevent duplicate URLs
        boolean urlValid = false;
        synchronized(validLock){
            urlValid = frontier.isValidURL(normalizedURL);
            if(urlValid && (currentDepth < settings.getMaxDepth())){
                usedUrls.put(normalizedURL, "");
            }
        }
        
        //add to urlDocMap which maps url to filename
        urlDocMap.add(url + " " + fileName);
        
        //mark URL as "used"
        usedUrls.put(url, fileName);

        
        return true;
    }
}
