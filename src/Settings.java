import java.nio.file.Path;

public interface Settings {
    Path getSeedPath();

    Path getStoragePath();

    int getNumPagesToCrawl();

    int getMaxDepth();

    int getNumThreads();
}
