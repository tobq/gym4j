package tobi.gym;

public class Discrete implements SpaceInstance {
    private final int i;

    public Discrete(int i) {
        this.i = i;
    }

    @Override
    public Object format() {
        return i;
    }
}
