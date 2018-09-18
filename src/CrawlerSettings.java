import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

class CrawlerSettings implements Settings {
    private Path seedPath;			//used to initialize the crawler
    private Path storagePath;		//used to store the crawled pages and the mapping of links to filenames

    private int numPagesToCrawl;
    private int maxDepth;
    private int numThreads;

    private final Path DEFAULT_PATH = Paths.get("./crawledPages");

    public CrawlerSettings(String[] args) {

        setSeedPath(null);
        setNumPagesToCrawl(0);
        setMaxDepth(0);
        setNumThreads(getNumThreadsInCPU());
        setStoragePath(DEFAULT_PATH);

        parseArguments(args);

        assert(seedPath != null);
        assert(numPagesToCrawl >= 0);
        assert(maxDepth >= 0);
    }

    private void parseArguments(String[] args){
        //todo: consider jcommander

        //prints error message if arguments are wrong then exits
        if(args.length < 3 || args.length > 4){
            System.out.println("Incorrect arguments passed. Arguments are of the form: \n"
                    + "[Seed file path] [# Pages to Crawl] [# of Levels][optional: page storage path]");
            System.exit(-1);
        }

        //Initializes the crawler settings
        try {
            setSeedPath(Paths.get(args[0]));
            setNumPagesToCrawl(Integer.parseInt(args[1]));
            setMaxDepth(Integer.parseInt(args[2]));
            if(args.length ==  4) setStoragePath(Paths.get(args[3]));
        }catch(NumberFormatException e){
            System.out.println("Are arg[1] and arg[2] numbers?");
            e.printStackTrace();
        }catch(InvalidPathException e){
            System.out.println("Invalid path");
            e.printStackTrace();
        }
    }

    /**
     * @return the number of threads in the CPU architecture
     */
    private int getNumThreadsInCPU() {
        int cores = Runtime.getRuntime().availableProcessors();
        System.out.println("cores: " + cores);
        return cores * 4;
    }

    @Override
    public Path getSeedPath() {
        return seedPath;
    }

    void setSeedPath(Path seedPath) {
        this.seedPath = seedPath;
    }

    @Override
    public Path getStoragePath() {
        return storagePath;
    }

    void setStoragePath(Path storagePath) {
        this.storagePath = storagePath;
    }

    @Override
    public int getNumPagesToCrawl() {
        return numPagesToCrawl;
    }

    void setNumPagesToCrawl(int numPagesToCrawl) {
        this.numPagesToCrawl = numPagesToCrawl;
    }

    @Override
    public int getMaxDepth() {
        return maxDepth;
    }

    void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    @Override
    public int getNumThreads() {
        return numThreads;
    }

    private void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }
}