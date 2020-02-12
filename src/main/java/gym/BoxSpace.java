package gym;

public class BoxSpace extends Space<Box> {
    private final int[] shape;
    private final double[] high;
    private final double[] low;

    public BoxSpace(int[] shape, double[] highs, double[] lows) {
        this.shape = shape;
        this.high = highs;
        this.low = lows;
    }

    public int[] getShape() {
        return shape;
    }

    public double[] getHigh() {
        return high;
    }

    public double[] getLow() {
        return low;
    }

    public int getArraySize() {
        int size = shape[0];
        for (int i = 1; i < shape.length; i++) {
            size *= shape[i];
        }
        return size;
    }
}
