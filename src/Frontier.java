public interface Frontier{

    public boolean add(FrontierElement fe);

    public boolean offer(FrontierElement fe);

    public FrontierElement remove();

    public FrontierElement poll();

    public FrontierElement element();

    public FrontierElement peek();
}
