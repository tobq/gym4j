package tobi.gym;

public class Discrete implements Action {
    private final int i;

    public Discrete(int i) {
        this.i = i;
    }

    @Override
    public Object format() {
        return i;
    }
}
