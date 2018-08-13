//todo: log the failed URLs
class FrontierElement implements Comparable<FrontierElement>{
    private String url;
    private int depth;

    FrontierElement(String url, int depth) {
        setUrl(url);
        setDepth(depth);
    }

    String getUrl() {
        return url;
    }

    private void setUrl(String url) {
        //if the URL doesn't have a protocol, attempts to fix it
        boolean hasProtocol = url.startsWith("http://") || url.startsWith("https://");
        this.url = hasProtocol ? url : "http://" + url;

        //todo: delete me
        if(!hasProtocol){
            System.err.println("ERROR: URL HAS NO PROTOCOL! Attempting recovery by prepending protocol");
        }
    }

    int getDepth() {
        return depth;
    }

    private void setDepth(int depth) {
        this.depth = depth;
    }

    /**
     * The comparison is based on the underlying URLs.
     *
     * @param frontierElement the frontier element to be compared.
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than
     * the specified object.
     */
    @Override
    public int compareTo(FrontierElement frontierElement) {
        if(frontierElement == null){
            throw new NullPointerException();
        }

        return this.getUrl().compareTo(frontierElement.getUrl());
    }
}