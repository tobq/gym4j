package gym;

import gym.util.Utils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

public class Environment<O extends Action, A extends Action> implements AutoCloseable {
    private static BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
    private static HashMap<Integer, Callback<byte[]>> messageCallbacks = new HashMap<>();
    private static boolean initialised = false;


    private final int id;
    private Space<A> actionSpace;
    private Space<O> observationSpace;

    public static void main(String[] args) throws InterruptedException, IOException, URISyntaxException {
        Environment.init();
        final Environment<Box, Box> env = new Environment<>("BipedalWalker-v2", true);
        System.out.println("env.reset() = " + env.reset());

        for (int i = 0; i < 300; i++) {
            System.out.println("env.step(new Box(1,1,1,1)) = " + env.step(new Box(1, 1, 1, 1)));
        }

        env.close();
    }

    public synchronized static void init() throws IOException, URISyntaxException, InterruptedException {
        if (initialised) return;

        final int THREAD_COUNT = 3;
        final Thread[] threads = new Thread[THREAD_COUNT];
        final CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        File file = new File(Environment.class.getResource("/gym/shell.py").toURI());
        ProcessBuilder builder = new ProcessBuilder("python", file.toString());
        Process process = builder.start();

        // WAITING THREAD (stops python continuing after this process closes)
        Runtime.getRuntime().addShutdownHook(new Thread(process::destroyForcibly));

        // RESPONSE READING THREAD
        threads[0] = Utils.startLoggedThread(() -> {
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
                Utils.startLoggedThread(() -> messageCallbacks.get(mid).call(response));
            }
        });

        // MESSAGE SENDING THREAD
        threads[1] = Utils.startLoggedThread(() -> {
            latch.countDown();

            final DataOutputStream writer = new DataOutputStream(process.getOutputStream());
            int messageCount = 0;
            while (true) {
                final Message message;
                message = messageQueue.take();
                final byte[] msg = message.getMessage();
                final int mid = messageCount++;
                messageCallbacks.put(mid, message.getCallback());
                writer.writeInt(mid);
                writer.writeInt(msg.length);
                writer.write(msg);
//                System.out.println("SENDING[" + mid + "]: " + Arrays.toString(msg));
                writer.flush();
            }
        });

        // ERROR READING THREAD
        threads[2] = Utils.startLoggedThread(() -> {
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

    public void fetchSpaces() throws IOException, InterruptedException {
        JSONObject event = makeEvent().put("type", "shape");
        final JSONObject response = sendWaitJSON(event);
        actionSpace = Space.parse(response.getJSONObject("action"));
        observationSpace = Space.parse(response.getJSONObject("observation"));
    }

    private JSONObject makeEvent() {
        return new JSONObject().put("id", id);
    }

    public Environment(String envId) throws IOException, InterruptedException {
        this(envId, false);
    }

    public Environment(String envId, boolean render) throws IOException, InterruptedException {
        JSONObject event = new JSONObject()
                .put("type", "make")
                .put("render", render)
                .put("envId", envId);

        final byte[] response = sendWait(event.toString());

        // BIG-ENDIAN
        this.id = (response[0] << 24) + (response[1] << 16) + (response[2] << 8) + response[3];
        // System.out.println("id = " + this.id);
        fetchSpaces();
    }

    private byte[] sendWait(String message) throws IOException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<byte[]> response = new AtomicReference<>();
        send(
                message,
                res -> {
                    response.set(res);
                    latch.countDown();

                });
        latch.await();
        return response.get();
    }

    private static void send(String message, Callback<byte[]> cb) throws IOException {
        if (!initialised) throw new RuntimeException("Environment not yet initialised. Call Environment#init()");
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(message.getBytes(StandardCharsets.UTF_8));
        // TODO: UTIL TO UTF BYTES
        out.close();
        messageQueue.add(new Message(
                out.toByteArray(),
                cb
        ));
    }

    public ActionResult<O> step(A action) throws IOException, InterruptedException {
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

    public O reset() throws IOException, InterruptedException {
        final JSONObject event = makeEvent().put("type", "reset");
        final JSONObject response = sendWaitJSON(event);
        return extractObservation(response);
    }

    private JSONObject sendWaitJSON(String message) throws IOException, InterruptedException {
        final byte[] recv = sendWait(message);
        return new JSONObject(new String(recv, StandardCharsets.UTF_8));
    }

    private JSONObject sendWaitJSON(JSONObject event) throws IOException, InterruptedException {
        return sendWaitJSON(event.toString());
    }

    //
    @Override
    public void close() throws IOException, InterruptedException {
        sendWaitJSON(makeEvent().put("type", "close"));
    }

    public Space<A> getActionSpace() {
        return actionSpace;
    }

    public Space<O> getObservationSpace() {
        return observationSpace;
    }
}

