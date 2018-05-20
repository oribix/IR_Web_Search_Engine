import java.io.*;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.ConcurrentLinkedQueue;

//TODO: Move the level Limit nonsense over to the threads
public class Crawler{
    //Thread variables
    private static String levelLimitChecker;		//Used to see if all threads are waiting

    //Each element hold value for each thread, when all are set to 1, there are no more valid urls within level limits
    private static AtomicIntegerArray levelLimits;
    
    //TODO: Redundant. Keep one, factor out the other
    //maps k<usedUrls> -> v<filename>
    //if the URL was not saved (i.e. has no filename) will hold null for v
    //private static ConcurrentSkipListMap<String, String> usedUrls;
    private static ConcurrentLinkedQueue<String> urlDocMap;     //maps URLs -> file names
    
    public static void main(String[] args) {
    	//Parses arguments!
    	ArgumentParser argumentParser = new ArgumentParser(args);
    	
        //Key variables
        Settings settings = argumentParser.getSettings();
        FileManager fileManager = new FileManager(settings);
        Frontier frontier = new Frontier(settings);
        
        //TODO: try to phase out or rework
        AtomicInteger pagesCrawled = new AtomicInteger(0);
        levelLimits = new AtomicIntegerArray(settings.getNumThreads());
        urlDocMap = new ConcurrentLinkedQueue<String>();
        //usedUrls = new ConcurrentSkipListMap<String, String>(); //TODO: delete me
        
        //create appropriate storage folder
        if(!fileManager.createStorageFolder()) {
            System.out.println("Storage folder creation failed. Aborting...");
            System.exit(-1);
        }

        //set LevelLimitChecker
        levelLimitChecker = "[1";
        for (int i = 1; i < settings.getNumThreads(); i++) {
            levelLimitChecker = levelLimitChecker + ", 1";
        }
        levelLimitChecker = levelLimitChecker + "]";

        //Instantiate threads and start them
        long startTime = System.nanoTime();
        CrawlerThread[] crawlerThreads = new CrawlerThread[settings.getNumThreads()];
        for(int i = 0; i < settings.getNumThreads(); i++){
            //TODO: GET THE DANG SIGNATURE CORRECT
            crawlerThreads[i] = new CrawlerThread(frontier, fileManager, settings, pagesCrawled, i, levelLimits, levelLimitChecker);
            crawlerThreads[i].start();
        }

        //saves mapping as we crawl pages
        //we do not go past this point until we have crawled all the pages
        FileWriter fw;
        BufferedWriter bw = null;
        PrintWriter writer = null;
        int pc = 0;        //pages crawled
        int pcols = 0;     //pages crawled on last save
        int ptcbs = 100;   //pages to crawl before we save
        while(((pc = pagesCrawled.get()) < settings.getNumPagesToCrawl()) && !(Objects.equals(levelLimits.toString(), levelLimitChecker))){
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
            String urlDocMapPath = settings.getStoragePath().toFile() + "/"+ "_url_doc_map.txt";
            writer = new PrintWriter(new BufferedWriter(new FileWriter(urlDocMapPath, true)));
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

        //synchronized(usedUrls){
        //    System.out.println(usedUrls);
        //}

        //Writes url-doc maps into a file once DocName Count reaches required amount
        //if (!url_doc_map.isEmpty() && docCount.get() == numPagesToCrawl) {
        //    writeMapTxt();
        //}
        System.out.print("Finished Crawler: ");
        if(Objects.equals(levelLimits.toString(), levelLimitChecker)) {
            System.out.println("Available pages have run out.");
        }
        else {
            System.out.println("Number of desired pages have been downloaded.");
        }
    }
}
