import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

class FrontierElem {
	private String url;         //the URL
	private int depth;          //depth the URL was discovered at

	/**
	 * @param url      The URL
	 * @param depth    Depth the URL was discovered at relative to the seed URLs
	 */
	public FrontierElem(String url, int depth) {
	    //TODO: clean up URL here. Throw an exception if URL cannot be cleaned
		this.url = url;
		this.depth = depth;
	}
	
	/**
     * @return the URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url the URL to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return the depth
     */
    public int getDepth() {
        return depth;
    }

    /**
     * @param depth the depth to set
     */
    public void setDepth(int depth) {
        this.depth = depth;
    }

    /**
     * @param base the base of the URL
     * @param url the full URL
     * @return the cleaned, normalized URL on success, else returns null
     */
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
}