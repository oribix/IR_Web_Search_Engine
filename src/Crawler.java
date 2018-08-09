import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.*;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.Semaphore;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class Crawler implements Runnable {

    private static Settings settings;
    
    //Thread variables
    private Thread t;                               //the Crawler object's thread
    private String threadName;                      //name of the thread
    private int threadNumb;							//thread number, from 0 to (numThreads - 1)				
    private static AtomicInteger pagesCrawled;      //# of pages we have crawled
    private static Semaphore pagesLeft;             //number of pages left to crawl
    private static AtomicLong docCount;             //# of created documents, used to generate document names
    private static Object validLock;                //lock used with url_doc_map
    private int WaitCount = 0;						//each thread has own WaitCount, goes up everytime frontier's empty
    private static String LevelLimitChecker;		//Used to see if all threads are waiting
    
    //Each element hold value for each thread, when all are set to 1, there are no more valid urls within level limits
    private static AtomicIntegerArray levelLimits;

    //queue holding all the URLs we will crawl and Levels of those URLs
    private static ConcurrentLinkedQueue<FrontierElement> frontier;
    
    //maps k<usedUrls> -> v<filename>
    //if the URL was not saved (i.e. has no filename) will hold null for v
    private static ConcurrentSkipListMap<String, String> usedUrls;
    
    private static ConcurrentLinkedQueue<String> url_doc_map;            //holds all url-document mappings
    
    //Crawler constructor
    Crawler(String name, int numb){
        threadName = name;
        threadNumb = numb;
    }
    
    //Uses a base URL to normalize the given URL. Also cleans the URL of useless things.
    //returns the cleaned, normalized URL on success, else returns null 
    private String normalizeURL(String base, String url) {
        URL normalizedURL = null;
        try{
            URL context = new URL(base);
            normalizedURL = new URL(context, url);
        } catch(MalformedURLException e){
            return null;
        }
        
        String protocol = normalizedURL.getProtocol();
        String host = normalizedURL.getHost();
        String path = normalizedURL.getPath();
        
        String result = protocol + "://" + host + path;
        try {
            result = java.net.URLDecoder.decode(result, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            //e.printStackTrace();
            result = null;
        } catch (IllegalArgumentException e) {
            result = null;
        }
        return result;
    }
    
    //checks if we have already crawled this URL
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
    
    //Returns false if the URL is not valid (i.e. we don't want it in the frontier)
    private boolean isValidURL(String url){
        if(url != null && url.startsWith("http://") && !isDuplicate(url)){
            return true;
        }
        return false;
    }
    
    //given a URL, generates a filename
    private String generateFileName() {
        return docCount.incrementAndGet() + ".html";
    }
    
    //saves the contents of a page into a file "filename." Uses the storangePath variable
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

    //downloads the page at the specified URL's location
    //returns true on success
    private boolean downloadFile(String url, int hops){
        boolean success = false;
        
        //request page with HTTP get
        Document doc = null;
        boolean getSuccess = false;
        try {
            doc = Jsoup.connect(url).get();
            getSuccess = true;
        } catch (IOException e) {
            //getting the doc failed...
            //e.printStackTrace();
        } catch (IllegalArgumentException e) {
            
        }
        
        if(getSuccess){
            //get the HTML content
            String htmlContent = doc.html();
            if(htmlContent != null){
                //saves the page in a file
                String fileName = saveAsFile(htmlContent);
                if( fileName != null){
                    //succeeded in saving html file, now add to url-doc_map string list
                    url_doc_map.add(url + " " + fileName);
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
                            if(urlValid && (hops < settings.getMaxDepth())){
                                usedUrls.put(normalizedURL, "");
                            }
                        }

                        //System.out.println(url + " has " + hops + " hops");
                        //if the url is valid, and isn't more hops away from see than numLevels
                        if(urlValid && (hops < settings.getMaxDepth())){
                            try{
                            	FrontierElement normFrontierElement = new FrontierElement(normalizedURL, hops + 1);
                                frontier.add(normFrontierElement);
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
    
	public static void main(String[] args) {

        //get the settings from the args
        settings =  new CrawlerSettings(args);

	    //initializing the variables
	    docCount = new AtomicLong(0);
	    pagesCrawled = new AtomicInteger(0);
	    frontier = new ConcurrentLinkedQueue<FrontierElement>();
	    url_doc_map = new ConcurrentLinkedQueue<String>();
	    usedUrls = new ConcurrentSkipListMap<String, String>();
	    levelLimits = new AtomicIntegerArray(settings.getNumThreads());
	    validLock = new Object();
	    
	    
	    //prints error message if arguments are wrong then exits
	    if(args.length < 3 || args.length > 4){
	        System.out.println("Incorrect arguments passed. Arguments are of the form: \n"
                    + "[Seed file path] [# Pages to Crawl] [# of Levels][optional: page storage path]");
            return;
	    }
	    
	    //sets the variables to the arguments
        try {
	        pagesLeft = new Semaphore(settings.getNumPagesToCrawl());
	        
	        //Creates a folder to store crawled pages
	        File dir = settings.getStoragePath().toFile();
            if(dir.mkdirs()) System.out.println("Storage folder successfully created");
            else if(!dir.exists()){
                System.out.println("Storage folder creation failed. Exiting...");
                return;
            }
        }catch(NumberFormatException e){
	        System.out.println("Are arg[1] and arg[2] numbers?");
            e.printStackTrace();
	    }catch(InvalidPathException e){
	        System.out.println("Invalid path");
	        e.printStackTrace();
	    }
	    
	    //initialize the frontier
        Scanner seedScanner = null;
        try {
            seedScanner = new Scanner(settings.getSeedPath());
            //gets all lines from the document specified in seedPath
            //enters each line into frontier queue
            while(seedScanner.hasNext()){
                String URL = seedScanner.next();
            	FrontierElement BothURL_Hop = new FrontierElement(URL, 0);
                frontier.add(BothURL_Hop);
                usedUrls.put(URL, "");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            seedScanner.close();
        }
        
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
	    BufferedWriter bw = null;
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
	                    String mapping = url_doc_map.poll();
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
            while((s = url_doc_map.poll()) != null){
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
	    
	    //synchronized(usedUrls){
	    //    System.out.println(usedUrls);
	    //}
	    
	    //Writes url-doc maps into a file once DocName Count reaches required amount
	    //if (!url_doc_map.isEmpty() && docCount.get() == numPagesToCrawl) {
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
            if(pagesLeft.tryAcquire()){
            	FrontierElement BothUrl_hop = frontier.poll(); //get next URL in queue
                
                
                if(BothUrl_hop != null) {
                	//frontier queue wasn't empty, so WaitCount resets
                	WaitCount = 0;
                	//levelLimits array shows this thread isn't waiting
                	levelLimits.compareAndSet(threadNumb, 1, 0);
                	
                	
                	String url = BothUrl_hop.getUrl();
                	
                    //if the URL doesn't have a protocol, attempts to fix it
                    if(!url.startsWith("http://") && !url.startsWith("https://")){
                        System.out.println("ERROR: URL HAS NO PROTOCOL! Attemping recovery by prepending protocol");
                        url  = "http://" + url;
                    }
                    
                    //downloads the URL
                    //try {
                        if(pagesCrawled.get() < settings.getNumPagesToCrawl()){
                            if(downloadFile(url, BothUrl_hop.getDepth())){
                                //keeps track of how many pages we have crawled
                                int p = pagesCrawled.incrementAndGet();
                                if(p % 100 == 0)System.out.println("Pages Crawled: " + p);
                            }
                            else pagesLeft.release(); //downloadFile failed. Release permit
                        }
                }
                else {
                	WaitCount++;
                	pagesLeft.release();//URL invalid. Release permit
                }
            }
            if (WaitCount > 1000) {
            	//if the current thread is waiting, set the int in the levelLimits array to 1
            	//this says that this thread isn't finding any pages in the queue
            	levelLimits.set(threadNumb, 1);
            }
        }
        return;
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
