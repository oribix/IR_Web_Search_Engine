import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class CrawlerThread extends Thread{
    
    private Settings settings;
    private int threadid;
    
    //shared thread variables
    private Frontier frontier;              //see Frontier for information
    private Semaphore pagesLeft;            //number of pages left to crawl
    private int waitCount;                  //each thread has own WaitCount, goes up every time frontier's empty
    
    
    //TODO: Phase these out or rework
    private AtomicIntegerArray levelLimits;
    
    private AtomicInteger pagesCrawled;     //number of pages successfully crawled so far
                                            //TODO: Maybe use FileManager.docCount?
    
    //CrawlerThread constructor
    CrawlerThread(Frontier frontier, AtomicInteger pagesCrawled, Settings settings, int threadid){
        this.frontier = frontier;
        this.pagesCrawled = pagesCrawled;
        this.settings = settings;
        this.threadid = threadid;
        
        this.pagesLeft = frontier.getPermits(); //TODO: Phase this out
        
    }
    
    //This handles the actions of the thread
    @Override
    public void run() {
        //While we havn't crawled enough pages or crawled within the specified depth
        while((pagesCrawled.get() < settings.getNumPagesToCrawl()) && !(Objects.equals(levelLimits.toString(), LevelLimitChecker))){
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
                    //try {
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
    
    /**
     * @param url URL to fetch doc from
     * @return On success returns the document which the URL points to, else null
     */
    private Document fetchDoc(String url) {
        //request page with HTTP get
        Document doc = null;
        try {
            doc = Jsoup.connect(url).get();
        } catch (IOException e) {
        } catch (IllegalArgumentException e) {
        }
        return doc;
    }
    
    /**
     * @param url The URL to check
     * @return true if the URL is fit for the frontier, false otherwise (i.e we do not want it in the frontier) 
     */
    private boolean isValidURL(String url){
        //TODO: investigate why "https://" is not here
        if(url != null && url.startsWith("http://") && !isDuplicate(url)){
            return true;
        }
        return false;
    }
    
    /**
     * @param url URL to check
     * @return true if the URL is a duplicate, false otherwise
     */
    private boolean isDuplicate(String url){
        String alt = null;
        int len = url.length();
        if(url.endsWith("/") && len > 1) alt = url.substring(0, len - 2);
        else alt = url + "/";
        synchronized (usedUrls) {
            if (usedUrls.get(url) != null || usedUrls.get(alt) != null) {
                //System.out.println(url + " is a duplicate!");
                return true;
            }
        }
        return false;
    }
    
    //downloads the page at the specified URL's location
    //returns true on success
    private boolean downloadFile(FrontierElem frontierElem){
        boolean success = false;

        Document doc = fetchDoc(frontierElem.getUrl());

        if(doc != null){
            //get the HTML content
            String htmlContent = doc.html();
            if(htmlContent != null){
                //saves the page in a file
                String fileName = saveAsFile(htmlContent);
                if( fileName != null){
                    //succeeded in saving html file, now add to url-doc_map string list
                    urlDocMap.add(url + " " + fileName);
                    usedUrls.put(url, fileName);

                    //Gets all the links in the page and add them into the frontier
                    Elements urlLinks = doc.select("a[href]");
                    for(Element elem : urlLinks){
                        String hrefURL = elem.attr("href");
                        String normalizedURL = normalizeURL(url, hrefURL);

                        //checks if a URL is valid. Records it if it is to prevent duplicate URLs
                        boolean urlValid = false;
                        synchronized(validLock){
                            urlValid = isValidURL(normalizedURL);
                            if(urlValid && (hops < numLevels)){
                                usedUrls.put(normalizedURL, "");
                            }
                        }

                        //System.out.println(url + " has " + hops + " hops");
                        //if the url is valid, and isn't more hops away from see than numLevels
                        if(urlValid && (hops < numLevels)){
                            try{
                                FrontierElem normURLPair = new FrontierElem(normalizedURL, hops + 1);
                                frontier.add(normURLPair);
                            } catch(NullPointerException e){
                                e.printStackTrace();
                            }
                        }
                    }

                    success = true;
                }
                else {
                    System.out.println("error saving document. url: " + url);
                }
            }
        }
        return success;
    }
}
