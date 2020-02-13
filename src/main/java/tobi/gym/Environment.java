package tobi.gym;

import tobi.gym.util.Utils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class Environment<O extends SpaceInstance, A extends SpaceInstance> implements AutoCloseable {
    private final int id;
    protected final Gym gym;
    private Space<A> actionSpace;
    private Space<O> observationSpace;

    private JSONObject makeEvent() {
        return new JSONObject().put("id", id);
    }

    public Environment(String envId, Gym gym) {
        this(envId, false, gym);
    }

    public Environment(String envId, boolean render, Gym gym) {
        this.gym = gym;
        JSONObject event = new JSONObject()
                .put("type", "make")
                .put("render", render)
//                .put("seed", seed) TODO: IMPLEMENT SEEDING
                .put("envId", envId);
        final byte[] response = sendWait(event.toString());
//        System.out.println("response = " + Arrays.toString(response));
        id = Utils.parseInt(response);
//        System.out.println("id = " + id);
        JSONObject event1 = makeEvent().put("type", "shape");
        final JSONObject response1 = sendWaitJSON(event1);

        actionSpace = Space.parse(response1.getJSONObject("action"));
        observationSpace = Space.parse(response1.getJSONObject("observation"));
    }

    private byte[] sendWait(String message) {
        final CompletableFuture<byte[]> future = new CompletableFuture<>();
        send(message, future::complete);
        return future.join();
    }

    private void send(String message, Callback<byte[]> cb) {
        gym.submit(new CallbackMessage(message, cb));
    }

    private void send(String message) {
        gym.submit(new Message(message));
    }

//    private static void assertInitialised() {
//        if (!initialised) throw new RuntimeException("Environment not yet initialised. Call Environment#init()");
//    }

    private void send(JSONObject event) {
        send(event.toString());
    }

    public ActionResult<O> step(A action) {
//        System.out.println("action.format() = " + action.format());
        final JSONObject event = makeEvent()
                .put("type", "step")
                .put("action", action.JSONFormat());


        final JSONObject response = sendWaitJSON(event);
        O observation = extractObservation(response);
        double reward = response.getDouble(ActionResult.KEY_REWARD);
        boolean done = response.getBoolean(ActionResult.KEY_DONE);
        return new ActionResult<>(observation, reward, done);
    }

    private O extractObservation(JSONObject response) {
        Object result = response.get(ActionResult.KEY_OBSERVATION);
        SpaceInstance res;
        if (result instanceof JSONArray) res = Utils.parseBox((JSONArray) result);
        else if (result instanceof Integer) res = new Discrete((Integer) result);
        else throw new IllegalArgumentException("Unrecognised observation: " + result);
        return (O) res;
    }

    public O reset() {
        final JSONObject event = makeEvent().put("type", "reset");
        final JSONObject response = sendWaitJSON(event);
        return extractObservation(response);
    }

    private JSONObject sendWaitJSON(String message) {
        final byte[] recv = sendWait(message);
        return new JSONObject(new String(recv, StandardCharsets.UTF_8));
    }

    private JSONObject sendWaitJSON(JSONObject event) {
        return sendWaitJSON(event.toString());
    }

    //
    @Override
    public void close() throws InterruptedException {
        send(makeEvent().put("type", "close"));
    }

    public Space<A> getActionSpace() {
        return actionSpace;
    }

    public Space<O> getObservationSpace() {
        return observationSpace;
    }
}

