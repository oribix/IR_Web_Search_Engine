
public class FrontierElement {
	private String url;
	private int depth;

	public FrontierElement(String url, int depth) {
		setUrl(url);
		setDepth(depth);
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}
}