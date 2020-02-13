package tobi.gym;

public class Discrete implements SpaceInstance {
    private final int value;

    public Discrete(int value) {
        this.value = value;
    }

    @Override
    public Object JSONFormat() {
        return value;
    }

    public int getValue() {
        return value;
    }
}
