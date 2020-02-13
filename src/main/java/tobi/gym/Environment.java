package tobi.gym;

import tobi.gym.util.BoxBoxEnvironment;
import tobi.gym.util.Utils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

public class Environment<O extends Action, A extends Action> implements AutoCloseable {
    private final int id;
    protected final Gym gym;
    private Space<A> actionSpace;
    private Space<O> observationSpace;

    public static void main(String[] args) throws InterruptedException, IOException, URISyntaxException {
        try (Gym gym = new Gym()) {
            try (BoxBoxEnvironment env = new BoxBoxEnvironment("BipedalWalker-v2", gym)) {
//            try (GymEnvironment<Box, Box> env = new GymEnvironment<Box,Box>("BipedalWalker-v2")) {
//            try (BoxBoxEnvironment env = Gym.makeBoxBox("BipedalWalker-v2", gym)) {
                final BoxSpace actionSpace = (BoxSpace) env.getActionSpace();
                final BoxSpace observationSpace = (BoxSpace) env.getObservationSpace();
                final Box initialState = env.reset();
                for (int i = 0; i < 1; i++) {
                    final ActionResult<Box> result = env.step(new Box(1, 1, 1, 1));
                    final Box observation = result.getObservation();
                    final double reward = result.getReward();
                    final boolean done = result.isDone();
                }
            }
        }
//        final Environment<Box, Box> env = new Environment<>("BipedalWalker-v2", true);
//        System.out.println("env.reset() = " + env.reset());
//
//        for (int i = 0; i < 300; i++) {
//            System.out.println("env.step(new Box(1,1,1,1)) = " + env.step(new Box(1, 1, 1, 1)));
//        }
//
//        env.close();
    }


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
                .put("action", action.format());


        final JSONObject response = sendWaitJSON(event);
        O observation = extractObservation(response);
        double reward = response.getDouble(ActionResult.KEY_REWARD);
        boolean done = response.getBoolean(ActionResult.KEY_DONE);
        return new ActionResult<>(observation, reward, done);
    }

    private O extractObservation(JSONObject response) {
        Object result = response.get(ActionResult.KEY_OBSERVATION);
        Action res;
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

