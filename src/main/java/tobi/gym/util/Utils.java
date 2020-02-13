package tobi.gym.util;

import tobi.gym.Box;
import org.json.JSONArray;

public class Utils {
    public static Box parseBox(JSONArray ob) {
        return new Box(toDoubleArray(ob));
    }

    public static double[] toDoubleArray(JSONArray ob) {
        int size = ob.length();
        double[] box = new double[size];
        for (int i = 0; i < size; i++) {
            double val;
            final Object serialised = ob.get(i);
            if (serialised instanceof String) {
                if (serialised.equals("Infinity"))
                    val = Double.POSITIVE_INFINITY;
                else if (serialised.equals("-Infinity"))
                    val = Double.NEGATIVE_INFINITY;
                else val = 0;
            } else val = ob.getDouble(i);
            box[i] = val;
        }
        return box;
    }

    public static Thread startLoggedThread(CaughtRunnable x) {
        final Thread thread = new Thread(() -> {
            try {
                x.run();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
        thread.start();
        return thread;
    }

    /**
     * Parse big-endian integer bytes
     *
     * @param response big endian bytes
     * @return corresponding integer
     */
    public static int parseInt(byte[] response) {
        return ((response[0] & 0xFF) << 24) + ((response[1] & 0xFF) << 16) + ((response[2] & 0xFF) << 8) + (response[3] & 0xFF);
    }
}

