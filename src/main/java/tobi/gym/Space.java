package tobi.gym;

import tobi.gym.util.Utils;
import org.json.JSONArray;
import org.json.JSONObject;

public class Space<Action> {
    public static Space parse(JSONObject space) {
        if (space.getString("type").equals("box")) {
            JSONArray ob = space.getJSONArray("shape");
            int size = ob.length();
            int[] shape = new int[size];
            for (int i = 0; i < size; i++) {
                shape[i] = ob.getInt(i);
            }
            return new BoxSpace(
                    shape,
                    Utils.toDoubleArray(space.getJSONArray("high")),
                    Utils.toDoubleArray(space.getJSONArray("low"))
            );
        }
        if (space.getString("type").equals("discrete"))
            return new DiscreteSpace(space.getInt("size"));
        else throw new IllegalArgumentException("Invalid space: " + space);
    }
}
