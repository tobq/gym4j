package tobi.gym;

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
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
    private static HashMap<Integer, Callback<byte[]>> messageCallbacks = new HashMap<>();
    private static boolean initialised = false;


    private final int id;
    private Space<A> actionSpace;
    private Space<O> observationSpace;

    public static void main(String[] args) throws InterruptedException, IOException, URISyntaxException {
        Environment.init();
//        final Environment<Box, Box> env = new Environment<>("BipedalWalker-v2", true);
//        System.out.println("env.reset() = " + env.reset());
//
//        for (int i = 0; i < 300; i++) {
//            System.out.println("env.step(new Box(1,1,1,1)) = " + env.step(new Box(1, 1, 1, 1)));
//        }
//
//        env.close();
    }

    public synchronized static void init() throws IOException, InterruptedException, URISyntaxException {
        if (initialised) return;

        final int THREAD_COUNT = 3;
//        final Thread[] threads = new Thread[THREAD_COUNT];
        final CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
//        final Path tempFile = Files.createTempFile("tobi.gym-", Long.toString(System.currentTimeMillis())).toAbsolutePath();
//
//        try (InputStream resourceStream = Environment.class.getResourceAsStream("/tobi/gym/shell.py")) {
//            Files.copy(resourceStream, tempFile);
//        }
//        System.out.println("tempFile = " + tempFile);
        ProcessBuilder builder = new ProcessBuilder("python", Utils.installGym());
        Process process = builder.start();

        Runtime.getRuntime().addShutdownHook(new Thread(process::destroyForcibly));

        // RESPONSE READING THREAD
        /*threads[0] = */
        Utils.startLoggedThread(() -> {
            final DataInputStream reader = new DataInputStream(process.getInputStream());
            latch.countDown();
            while (true) {
                int mid = reader.readInt();
//                System.out.println("RESPONSE: mid = " + mid);
                int length = reader.readInt();
//                System.out.println("RESPONSE: length = " + length);
                final byte[] response = new byte[length];
//                System.out.println("RESPONSE = " + Arrays.toString(response));
                reader.readFully(response);
                final Callback<byte[]> callback = messageCallbacks.get(mid);
                messageCallbacks.remove(mid);
                Utils.startLoggedThread(() -> callback.call(response));
            }
        });

        // MESSAGE SENDING THREAD
        /*threads[1] = */
        Utils.startLoggedThread(() -> {
            latch.countDown();

            final DataOutputStream writer = new DataOutputStream(process.getOutputStream());
            int messageCount = 0;
            while (true) {
                final Message message = messageQueue.take();
                final byte[] msg = message.getMessage();
                final int mid = messageCount++;
                if (message instanceof CallbackMessage)
                    messageCallbacks.put(mid, ((CallbackMessage) message).getCallback());
                writer.writeInt(mid);
                writer.writeInt(msg.length);
                writer.write(msg);
//                System.out.println("SENDING[" + mid + "]: " + Arrays.toString(msg));
                writer.flush();
            }
        });

        // ERROR READING THREAD
        /*threads[2] = */
        Utils.startLoggedThread(() -> {
            latch.countDown();

            final BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line;
            while (true) {
                while ((line = errorReader.readLine()) != null) System.out.println(line);
            }
        });

        latch.await();
        initialised = true;
    }

    private JSONObject makeEvent() {
        return new JSONObject().put("id", id);
    }

    public Environment(String envId) {
        this(envId, false);
    }

    public Environment(String envId, boolean render) {
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

    private static void send(String message, Callback<byte[]> cb) {
        assertInitialised();
        messageQueue.add(new CallbackMessage(message, cb));
    }

    private static void send(String message) {
        assertInitialised();
        messageQueue.add(new Message(message));
    }

    private static void assertInitialised() {
        if (!initialised) throw new RuntimeException("Environment not yet initialised. Call Environment#init()");
    }

    private static void send(JSONObject event) {
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
    public void close() {
        send(makeEvent().put("type", "close"));
    }

    public Space<A> getActionSpace() {
        return actionSpace;
    }

    public Space<O> getObservationSpace() {
        return observationSpace;
    }
}

