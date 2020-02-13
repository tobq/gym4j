package tobi.gym;

import org.json.JSONArray;

import java.util.Arrays;

public class Box implements SpaceInstance {
    private final double[] values;
//    private final double[] dimensions;

    public Box(double... values) {
        this.values = values;
    }

    public JSONArray JSONFormat() {
        JSONArray result = new JSONArray();
        for (double value : values) {
            result.put(value);
        }
        return result;
    }

    public double[] toArray() {
        return values;
    }

    public double get(BoxSpace space, int... indices) {
        final int[] shape = space.getShape();

        if (values.length != space.getArraySize() || indices.length != shape.length)
            throw new IllegalArgumentException("Invalid BoxSpace used to get value at " + Arrays.toString(indices));

        int index = 0;
        for (int dimension = 0; dimension < indices.length; dimension++) {
            index = indices[dimension] * shape[dimension];
        }

        return values[index];
    }
}
