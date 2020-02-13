package tobi.gym;

import tobi.gym.util.BoxBoxEnvironment;
import tobi.gym.util.BoxDiscreteEnvironment;
import tobi.gym.util.DiscreteDiscreteEnvironment;
import tobi.gym.util.Utils;

import java.io.*;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

public class Gym implements AutoCloseable {
    private static final int THREAD_COUNT = 3;
    private BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
    private HashMap<Integer, Callback<byte[]>> messageCallbacks = new HashMap<>();
    private CountDownLatch shutdownLatch = new CountDownLatch(THREAD_COUNT);
    private volatile boolean running = true;
    private Process process;

    public Gym() throws IOException, InterruptedException, URISyntaxException {
        final CountDownLatch startupLatch = new CountDownLatch(THREAD_COUNT);
        ProcessBuilder builder = new ProcessBuilder("python", Utils.installGym());
        process = builder.start();

        Runtime.getRuntime().addShutdownHook(new Thread(process::destroyForcibly));

        // RESPONSE READING THREAD
        Utils.startLoggedThread(() -> {
            final DataInputStream reader = new DataInputStream(process.getInputStream());
            startupLatch.countDown();
            out:
            while (running) {
                while (reader.available() < 4) if (!running) break out;
                int mid = reader.readInt();
//                System.out.println("RESPONSE: mid = " + mid);
                while (reader.available() < 4) if (!running) break out;
                int length = reader.readInt();
//                System.out.println("RESPONSE: length = " + length);
                while (reader.available() < length) if (!running) break out;
                final byte[] response = new byte[length];
//                System.out.println("RESPONSE = " + Arrays.toString(response));
                reader.readFully(response);
                if (messageCallbacks.containsKey(mid)) {
                    final Callback<byte[]> callback = messageCallbacks.get(mid);
                    messageCallbacks.remove(mid);
                    Utils.startLoggedThread(() -> callback.call(response));
                }
            }
            shutdownLatch.countDown();
//            System.out.println(shutdownLatch.getCount());
        });

        // MESSAGE SENDING THREAD
        Utils.startLoggedThread(() -> {
            startupLatch.countDown();

            final DataOutputStream writer = new DataOutputStream(process.getOutputStream());
            int messageCount = 0;
            while (running) {
                while (!messageQueue.isEmpty()) {
                    final Message message = messageQueue.remove();
                    final byte[] msg = message.getMessage();
                    final int mid = messageCount++;
                    if (message instanceof CallbackMessage)
                        messageCallbacks.put(mid, ((CallbackMessage) message).getCallback());
                    writer.writeInt(mid);
                    writer.writeInt(msg.length);
                    writer.write(msg);
                    writer.flush();
                }
            }
            shutdownLatch.countDown();
//            System.out.println(shutdownLatch.getCount());
        });

        // ERROR READING THREAD
        Utils.startLoggedThread(() -> {
            startupLatch.countDown();
            final BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line;
            while (running) {
                while (errorReader.ready() && (line = errorReader.readLine()) != null) {
                    System.err.println(line);
                }
            }
            shutdownLatch.countDown();
        });

        startupLatch.await();
    }

    final void submit(Message message) {
        messageQueue.add(message);
    }

    @Override
    public void close() throws InterruptedException {
        System.out.println("SHUTTING DOWN");
        running = false;
        shutdownLatch.await();
        process.destroy();
        System.out.println("STOPPED");
    }

    public static GymEnvironment make(String envId) throws InterruptedException, IOException, URISyntaxException {
        return new GymEnvironment(envId);
    }

    public static GymEnvironment make(String envId, boolean render) throws InterruptedException, IOException, URISyntaxException {
        return new GymEnvironment(envId, render);
    }

    public static Environment make(String envId, boolean render, Gym gym) {
        return new Environment(envId, render, gym);
    }

    public static Environment make(String envId, Gym gym) {
        return new Environment(envId, gym);
    }

    public static BoxBoxEnvironment makeBoxBox(String envId, Gym gym) {
        return new BoxBoxEnvironment(envId, gym);
    }

    public static BoxDiscreteEnvironment makeBoxDiscrete(String envId, Gym gym) {
        return new BoxDiscreteEnvironment(envId, gym);
    }

    public static DiscreteDiscreteEnvironment makeDiscreteDiscrete(String envId, Gym gym) {
        return new DiscreteDiscreteEnvironment(envId, gym);
    }
}
