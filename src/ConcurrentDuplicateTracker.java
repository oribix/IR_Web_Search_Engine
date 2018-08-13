import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class ConcurrentDuplicateTracker implements DuplicateTracker{
    private Set<FrontierElement> set;

    public ConcurrentDuplicateTracker(){
        set = new ConcurrentSkipListSet<>();
    }

    //returns true if the set did not already contain the element.
    @Override
    public boolean add(FrontierElement fe) {
        boolean success;
        try {
            success = set.add(fe);
        } catch (RuntimeException e){
            e.printStackTrace();
            success = false;
        }

        return success;
    }
}
