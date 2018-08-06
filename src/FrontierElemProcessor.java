import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Takes a frontier element then processes it for information
 *
 */

public class FrontierElemProcessor {
    //variables pertaining to the Frontier Element injected in
    private final String url;
    private final int currentDepth;
    private final Document document;
    
    //variables pertaining to the generated Frontier elements
    private List<FrontierElem> generatedFrontierElems;
    private final int nextDepth;
    
    
    public FrontierElemProcessor(FrontierElem frontierElem) {
        url = frontierElem.getUrl();
        currentDepth = frontierElem.getDepth();
        nextDepth = currentDepth + 1;
        
        generatedFrontierElems = new ArrayList<FrontierElem>();
        
        //fetch the document
        document = fetchDoc(url);
    }
    
    /**
     * @return the Frontier Elements generated from the given Frontier Element
     */
    FrontierElem[] getFrontierElems() {
        if(document == null) return null;
        
        //TODO: use Elements.eachAttr(String)
        
        Elements urlLinks = document.select("a[href]");
        for(Element elem : urlLinks){
            String hrefURL = elem.attr("href");
            String normalizedURL = FrontierElem.normalizeURL(url, hrefURL);
            FrontierElem frontierElem = new FrontierElem(normalizedURL, nextDepth);
            generatedFrontierElems.add(frontierElem);
//            
//            
//            //if the url is valid, and isn't more hops away from see than numLevels
//            if(urlValid && (currentDepth < settings.getMaxDepth())){
//                try{
//                    FrontierElem normURLPair = new FrontierElem(normalizedURL, currentDepth + 1);
//                    frontier.add(normURLPair);
//                } catch(NullPointerException e){
//                    e.printStackTrace();
//                }
//            }
        }
        
        
        return null;
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
}