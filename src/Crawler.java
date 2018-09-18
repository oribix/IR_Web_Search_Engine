import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.*;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.Semaphore;

//Jsoup
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class Crawler implements Runnable {

    //Key Crawler Constructs
    private static Settings settings;       //General Crawler Settings parsed from the command line
    private static Frontier frontier;       //Manages the urls to be crawled
    
    //Local Thread Variables
    private Thread t;               //the Crawler object's thread
    private String threadName;      //name of the thread
    private int threadNumb;			//thread number, from 0 to (numThreads - 1)
    private int WaitCount = 0;		//each thread has own WaitCount, goes up everytime frontier's empty

    //Shared Thread variables
    private static AtomicInteger pagesCrawled;      //# of pages we have crawled
    private static Semaphore downloadPermits;       //number of pages left to crawl
    private static AtomicLong docCount;             //# of created documents, used to generate document names
    private static String LevelLimitChecker;		//Used to see if all threads are waiting

    private static ConcurrentLinkedQueue<String> urlDocMap;   //holds all url-document mappings
    
    //Each element hold value for each thread, when all are set to 1, there are no more valid urls within level limits
    private static AtomicIntegerArray levelLimits;

//    //maps k<usedUrls> -> v<filename>
//    //if the URL was not saved (i.e. has no filename) will hold null for v
//    private static ConcurrentSkipListMap<String, String> usedUrls;
    
    //Crawler constructor
    public Crawler(String name, int numb){
        threadName = name;
        threadNumb = numb;
    }
    
    //Uses a base URL to normalize the given URL. Also cleans the URL of useless things.
    //returns the cleaned, normalized URL on success, else returns null 
    private String normalizeURL(String base, String url) {//todo: come back later
        URL normalizedURL;
        try{
            URL context = new URL(base);
            normalizedURL = new URL(context, url);
        } catch(MalformedURLException e){
            //e.printStackTrace(); //SO MANY EXCEPTIONS
            return null;
        }
        
        String protocol = normalizedURL.getProtocol();
        String host = normalizedURL.getHost();
        String path = normalizedURL.getPath();
        String result = protocol + "://" + host + path;
        try {
            //replaces"%xy" characters with real characters
            result = java.net.URLDecoder.decode(result, "UTF-8");
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            e.printStackTrace();
            result = null;
        }
        return result;
    }

    //todo: used with isDuplicate. This function's future lies in what happens to isDuplicate
//    //Some URL's end in '/' while others don't. Many times these are duplicates
//    //with the sole difference being the '/'
//    private String getAltUrl(String url){
//        String alt;
//        int len = url.length();
//        if(url.endsWith("/") && len > 1) {
//            alt = url.substring(0, len - 2); //todo: len-1?
//        }
//        else {
//            alt = url + "/";
//        }
//        return alt;
//    }

    //todo: test if '/' is really needed.
    //this code checked for duplicates that ended in '/' since some links ended in '/' and other didn't
    //it may be that this check is unnecessary and that I could just omit the '/' at the end of links
//    //checks if we have already crawled this URL
//    private boolean isDuplicate(String url){
//        String alt = getAltUrl(url);
//        synchronized (usedUrls) {
//            if (usedUrls.get(url) != null || usedUrls.get(alt) != null) {
//                //System.out.println(url + " is a duplicate!");
//                return true;
//            }
//        }
//        return false;
//    }

    //Returns false if the URL is not valid (i.e. we don't want it in the frontier)
    private boolean isValidURL(String url){
        //todo: why no https? Check also may be unnecessary
        return url != null && url.startsWith("http://");

        //if(isDuplicate(url)) return false; //todo: delete me after isDuplicate is checked.
    }
    
    //given a URL, generates a filename
    private String generateFileName() {
        return docCount.incrementAndGet() + ".html";
    }

    //saves the contents of a page into a file "filename." Uses the storagePath variable
    //returns the fileName on success, otherwise returns null
    private String saveAsFile(String htmlContent){
    	//System.out.println("filename is " + fileName);
    	try{
    	    String fileName = generateFileName();
    	    PrintWriter writer = new PrintWriter(settings.getStoragePath() + "/"+ fileName);
    	    writer.print(htmlContent);
    	    writer.close();
    	    return fileName;
    	} catch (FileNotFoundException e) {
    		e.printStackTrace();
    		return null;
    	}
    }

    //returns the Document at the specified url, null on failure
    private Document getDoc(String url){
        //request page with HTTP get
        Document doc = null;
        try {
            doc = Jsoup.connect(url).get();
        } catch (IOException | IllegalArgumentException e) {
            //e.printStackTrace();
        }

        return doc;
    }

    //downloads the page at the specified URL's location
    //returns true on success
    private boolean downloadFile(FrontierElement frontierElement){
        String url = frontierElement.getUrl();
        int depth = frontierElement.getDepth();

        //request page with HTTP get
        Document doc = getDoc(frontierElement.getUrl());
        if(doc == null) return false;

        //get the HTML content
        String htmlContent = doc.html();
        if(htmlContent == null) return false;

        //saves the page in a file
        String fileName = saveAsFile(htmlContent);

        if (fileName == null) {
            System.out.println("error saving document. url: " + url);
            return false;
        }

        //succeeded in saving html file, now add to urlDocMap string list
        urlDocMap.add(url + " " + fileName);
        //usedUrls.put(url, fileName); //todo: Check for correctness

        //Gets all the links in the page and add them into the frontier
        Elements urlLinks = doc.select("a[href]");//todo: get all links by href
        for(Element elem : urlLinks){
            String hrefURL = elem.attr("href");
            String normalizedURL = normalizeURL(url, hrefURL);

            //checks if a URL is valid. Records it if it is to prevent duplicate URLs
            boolean urlValid = isValidURL(normalizedURL);
            //todo: delete me after wellness check
//            if(urlValid && (depth < settings.getMaxDepth())){
//                usedUrls.put(normalizedURL, "");
//            }

            //if the url is valid, and isn't more depth away from see than numLevels
            if(urlValid && (depth < settings.getMaxDepth())){
                try{
                    FrontierElement normFrontierElement = new FrontierElement(normalizedURL, depth + 1);
                    frontier.add(normFrontierElement);
                } catch(NullPointerException e){
                    e.printStackTrace();
                }
            }
        }

        return true;
    }

    //Creates a folder to store crawled pages
    private static void createStorageFolder(Path storagePath){
        File dir = storagePath.toFile();
        if(dir.mkdirs()) System.out.println("Storage folder successfully created");
        else if(!dir.exists()){
            System.out.println("Storage folder creation failed. Exiting...");
            System.exit(-1);
        }
    }

	public static void main(String[] args) {

        //get the settings from the args
        settings =  new CrawlerSettings(args);

	    //initializing the variables
	    docCount = new AtomicLong(0);
	    pagesCrawled = new AtomicInteger(0);
	    frontier = new FrontierQueue(settings);
	    urlDocMap = new ConcurrentLinkedQueue<>();
//	    usedUrls = new ConcurrentSkipListMap<String, String>();
	    levelLimits = new AtomicIntegerArray(settings.getNumThreads());

        //sets the variables to the arguments
        downloadPermits = new Semaphore(settings.getNumPagesToCrawl());

        createStorageFolder(settings.getStoragePath());

        //set LevelLimitChecker
        LevelLimitChecker = "[1";
        for (int i = 1; i < settings.getNumThreads(); i++) {
        	LevelLimitChecker = LevelLimitChecker + ", 1";
        }
        LevelLimitChecker = LevelLimitChecker + "]";
        
        //creates Crawlers to be used as threads then runs them
        long startTime = System.nanoTime();
	    Crawler[] c = new Crawler[settings.getNumThreads()];
	    for(int i = 0; i < settings.getNumThreads(); i++){
	        c[i] = new Crawler("Thread " + i, i);
	        c[i].start();
	    }
	    
	    //saves mapping as we crawl pages
	    //we do not go past this point until we have crawled all the pages
	    FileWriter fw;
	    BufferedWriter bw;
	    PrintWriter writer = null;
	    int pc = 0;        //pages crawled
	    int pcols = 0;     //pages crawled on last save
	    int ptcbs = 100;   //pages to crawl before we save
	    while(((pc = pagesCrawled.get()) < settings.getNumPagesToCrawl()) && !(Objects.equals(levelLimits.toString(), LevelLimitChecker))){
    	    int pcsls = pc - pcols; //pages crawled since last save
	        if(pcsls >= ptcbs){
	            try{
	                fw = new FileWriter(settings.getStoragePath() + "/"+ "_url_doc_map.txt", true);
	                bw = new BufferedWriter(fw);
	                writer = new PrintWriter(bw);
	                for(int i = 0; i < pcsls; ){
	                    String mapping = urlDocMap.poll();
	                    if(mapping != null) {
	                        writer.println(mapping);
	                        i++;
	                    }
	                }
	                System.out.println("saved progress");
	            } catch(IOException e){
	               System.out.println("Error while saving progress");
	               e.printStackTrace();
	            } finally{
	                writer.close();
	                pcols = pc;
	            }
    	    }
	    }
	    
	    //save any leftovers
	    try {
	        fw = new FileWriter(settings.getStoragePath() + "/"+ "_url_doc_map.txt", true);
            bw = new BufferedWriter(fw);
            writer = new PrintWriter(bw);
            String s = null;
            while((s = urlDocMap.poll()) != null){
                writer.println(s);
            }
        } catch (IOException e) {
            System.out.println("Error while saving progress");
            e.printStackTrace();
        } finally{
            writer.close();
        }
	    
	    //prints how long it took to crawl all the pages
	    long endTime = System.nanoTime();
	    System.out.println("seconds: " + (endTime - startTime) / 1000000000);
	    
	    //Writes url-doc maps into a file once DocName Count reaches required amount
	    //if (!urlDocMap.isEmpty() && docCount.get() == numPagesToCrawl) {
	    //    writeMapTxt();
    	//}
	    System.out.print("Finished Crawler: ");
	    if(Objects.equals(levelLimits.toString(), LevelLimitChecker)) {
	    	System.out.println("Avalible pages have run out.");
	    }
	    else {
	    	System.out.println("Number of desired pages have been downloaded.");
	    }
	}

    //This handles the actions of the thread
    @Override
    public void run() {
    	//While we havn't either collecting the number of page, or have reached all the pages within our level limits
        while((pagesCrawled.get() < settings.getNumPagesToCrawl()) && !(Objects.equals(levelLimits.toString(), LevelLimitChecker))){
            if(downloadPermits.tryAcquire()){
                FrontierElement frontierElement = frontier.poll(); //get next URL in queue

                if(frontierElement != null) {
                    //frontier queue wasn't empty, so WaitCount resets
                    WaitCount = 0;
                    //levelLimits array shows this thread isn't waiting
                    levelLimits.compareAndSet(threadNumb, 1, 0);

                    //downloads the URL
                    if(pagesCrawled.get() < settings.getNumPagesToCrawl()){
                        if(downloadFile(frontierElement)){
                            //keeps track of how many pages we have crawled
                            int p = pagesCrawled.incrementAndGet();
                            if(p % 100 == 0)System.out.println("Pages Crawled: " + p);
                        }
                        else downloadPermits.release(); //downloadFile failed. Release permit
                    }
                }
                else {
                    WaitCount++;
                    downloadPermits.release();//URL invalid. Release permit
                }
            }
            if (WaitCount > 1000) {
            	//if the current thread is waiting, set the int in the levelLimits array to 1
            	//this says that this thread isn't finding any pages in the queue
            	levelLimits.set(threadNumb, 1);
            }
        }
    }
    
    //call this to start a thread
    public void start() {
        System.out.println("Starting " + threadName);
        if(t == null) {
            t = new Thread(this, threadName);
            t.start();
        }
        
    }
}
