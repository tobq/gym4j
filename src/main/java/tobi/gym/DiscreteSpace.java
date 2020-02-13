package tobi.gym;

public class DiscreteSpace extends Space<Discrete> {
    private final int size;

    public DiscreteSpace(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }
}
