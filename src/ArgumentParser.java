import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

/**
 * Argument Parser parses the passed in arguments.
 * As it stands, the parser simply uses the arguments to construct a Settings object. 
 *
 */
public class ArgumentParser {
	
	private String[] args;
	private Settings settings;
	
	public ArgumentParser(String[] args) {
		this.args = args;
		settings = new Settings();
	}
	
	public Settings getSettings() {
		//prints error message if arguments are wrong then exits
        if(args.length < 3 || args.length > 4){
            System.out.println("Incorrect arguments passed. Arguments are of the form: \n"
                    + "[Seed file path] [# Pages to Crawl] [# of Levels][optional: page storage path]");
            System.exit(-1);
        }

        //Initializes the crawler settings
        try {
            settings.setSeedPath(Paths.get(args[0]));
            settings.setNumPagesToCrawl(Integer.parseInt(args[1]));
            settings.setMaxDepth(Integer.parseInt(args[2]));
            if(args.length ==  4) settings.setStoragePath(Paths.get(args[3]));
        }catch(NumberFormatException e){
            System.out.println("Are arg[1] and arg[2] numbers?");
            e.printStackTrace();
        }catch(InvalidPathException e){
            System.out.println("Invalid path");
            e.printStackTrace();
        }
        
		return settings;
	}
}
