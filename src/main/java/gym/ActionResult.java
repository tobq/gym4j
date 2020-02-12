package gym;

public class ActionResult<O extends Action> {
    public static final String KEY_OBSERVATION = "observation";
    public static final String KEY_REWARD = "reward";
    public static final String KEY_DONE = "done";

    protected final double reward;
    protected final boolean done;
    protected final O observation;

    protected ActionResult(O observation, double reward, boolean done) {
        this.reward = reward;
        this.done = done;
        this.observation = observation;
    }

//    protected ActionResult(JSONObject result) {
//        this.result = result;
//        reward = result.getDouble(KEY_REWARD);
//        done = result.getBoolean(KEY_DONE);
//    }

    public O getObservation() {
        return observation;
    }

    public double getReward() {
        return reward;
    }

    public boolean isDone() {
        return done;
    }
}
