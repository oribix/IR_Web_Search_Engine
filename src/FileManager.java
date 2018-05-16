import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;

public class FileManager {
    
    private Settings settings;           		//Crawler Settings
    private static AtomicInteger docCount;      //keeps track of number of documents. Also used to name them.
    
    public FileManager(Settings settings) {
        docCount = new AtomicInteger(0);
        this.settings = settings;
    }

    //Creates a folder to store crawled pages
    public boolean createStorageFolder() {
        boolean success = true;
        
        File dir = settings.getStoragePath().toFile();
        if(dir.mkdirs()) {
            System.out.println("Storage folder created at " + settings.getStoragePath().toString());
        }
        else if(!dir.exists()) {
            success = false;
        }
        
        return success;
    }
    
    
    /**
     * Note: also helps keep track of number of documents saved
     * 
     * @return unique file name
     */
    private String generateFileName() {
        //TODO: Name based on SHA256 given URL
        return docCount.incrementAndGet() + ".html";
    }
    
    /**
     * @param htmlContent contents of a web page as a string
     * @return file name on success, null on failure
     */
    public String saveAsFile(String htmlContent){
        //System.out.println("filename is " + fileName);
        String fileName = generateFileName();
        String filePath = settings.getStoragePath() + "/"+ fileName;
        PrintWriter writer = null;
        try{
            writer = new PrintWriter(filePath);
            writer.print(htmlContent);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            fileName = null;
        } finally {
            writer.close();
        }
        return fileName;
    }
}
