import java.nio.file.Path;
import java.nio.file.Paths;

class Settings {
    private Path seedPath;			//used to initialize the crawler
    private Path storagePath;		//used to store the crawled pages and the mapping of links to filenames
    
    private int numPagesToCrawl;
    private int maxDepth;
    private int numThreads;
    
    private final Path DEFAULT_PATH = Paths.get("./crawledPages");
    
    public Settings() {
        setSeedPath(null);
        setNumPagesToCrawl(0);
        setMaxDepth(0);
        setNumThreads(getNumThreadsInCPU());
        storagePath = DEFAULT_PATH;
    }
    
    
    /**
     * @return the number of threads in the CPU architecture
     */
    private int getNumThreadsInCPU() {
        //TODO: get number of threads in CPU
        return 4;
    }

    /**
     * @return the seedPath
     */
    public Path getSeedPath() {
        return seedPath;
    }

    /**
     * @param seedPath the seedPath to set
     */
    public void setSeedPath(Path seedPath) {
        this.seedPath = seedPath;
    }

    /**
     * @return the numPagesToCrawl
     */
    public int getNumPagesToCrawl() {
        return numPagesToCrawl;
    }
    
    /**
     * @param numPagesToCrawl the numPagesToCrawl to set
     */
    public void setNumPagesToCrawl(int numPagesToCrawl) {
        this.numPagesToCrawl = numPagesToCrawl;
    }

    /**
     * @return the numThreads
     */
    public int getNumThreads() {
        return numThreads;
    }
    
    /**
     * @param numThreads the numThreads to set
     */
    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }
    

    /**
     * @return the numLevels
     */
    public int getMaxDepth() {
        return maxDepth;
    }

    /**
     * @param maxDepth the numLevels to set
     */
    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    /**
     * @return the storagePath
     */
    public Path getStoragePath() {
        return storagePath;
    }

    /**
     * @param storagePath the storagePath to set
     */
    public void setStoragePath(Path storagePath) {
        this.storagePath = storagePath;
    }
}
