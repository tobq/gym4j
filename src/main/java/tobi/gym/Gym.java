package tobi.gym;

import tobi.gym.util.*;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;

public class Gym implements AutoCloseable {
    private static final boolean LOG_PYTHON_ERRORS = true;
    private static final int THREAD_COUNT;

    static {
        int threadCount = 1;
        if (LOG_PYTHON_ERRORS) threadCount++;
        THREAD_COUNT = threadCount;
    }

    private static final String GYM_PYTHON_FOLDER = "/tobi/gym/gympy/";
    private static final String SHELL_PY_REL_PATH = "shell.py";
    private static final String PYTHON_SHELL_PATH = GYM_PYTHON_FOLDER + SHELL_PY_REL_PATH;
    private static final boolean THREADED_PYTHON_EXECUTION = true;

    private Runnable shutdownUninstall;
    private final Runnable shutdownDestroySubprocess;
    private final Thread shutdownHook;
    private final HashMap<Integer, CompletableFuture<byte[]>> messageCallbacks = new HashMap<>();
    private final CountDownLatch shutdownLatch = new CountDownLatch(THREAD_COUNT);
    private volatile boolean running = true;
    private final Process process;
    private final boolean pythonWasInstalled;
    private final DataOutputStream writer;

    public Gym() throws IOException, InterruptedException, URISyntaxException {
        this("python");
    }

    public Gym(String pythonExecutable) throws IOException, InterruptedException, URISyntaxException {
        // Latch used to block instantiation until all threads are up and running
        final CountDownLatch startupLatch = new CountDownLatch(THREAD_COUNT);
        String pythonShellPath;
        URL currentRuntimeLocation = Environment.class.getProtectionDomain().getCodeSource().getLocation();
        Path currentRuntimeLocationPath = Paths.get(currentRuntimeLocation.toURI());
        // If this runtime is running from a jar, the python project is installed to a tempo folder
        pythonWasInstalled = currentRuntimeLocation.getPath().endsWith(".jar");
        if (pythonWasInstalled) {
            // Explore the filesystem within this jar
            try (FileSystem jarFileSystem = FileSystems.newFileSystem(currentRuntimeLocationPath, (ClassLoader) null)) {
                Path pythonProjectInJar = jarFileSystem.getPath(GYM_PYTHON_FOLDER);
                final Path tempInstallDir = Files.createTempDirectory("tobi.gym.gympy-");
                // The python project within the jar is recursively copied over to the install jar
                copyJarDir(pythonProjectInJar, pythonProjectInJar, tempInstallDir);

                // A shutdown handler is configured to uninstall this temp python project
                shutdownUninstall = () -> {
                    try {
                        // file tree of the temp install dir is walked, with everything being deleted on the way
                        Files.walkFileTree(tempInstallDir, new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                Files.delete(file);
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                                if (e == null) {
                                    Files.delete(dir);
                                    return FileVisitResult.CONTINUE;
                                }
                                throw e;
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                };

                pythonShellPath = tempInstallDir.resolve(SHELL_PY_REL_PATH).toString();
            }
        } else {
            // If this is running locally without being zipped, the project can be accessed without installation
            pythonShellPath = new File(Environment.class.getResource(PYTHON_SHELL_PATH).getFile()).toString();
        }

        // The python process is then initialised with the configured location of the project
        final List<String> args = new ArrayList<>(Arrays.asList(pythonExecutable, pythonShellPath));
        if (THREADED_PYTHON_EXECUTION) args.add("--threaded");
        ProcessBuilder builder = new ProcessBuilder(args);
        process = builder.start();
        writer = new DataOutputStream(process.getOutputStream());

        // This synchronously shuts down the python process
        shutdownDestroySubprocess = () -> {
            process.destroyForcibly();
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        // The function is added as a shutdown hook.
        // The actual function is also stored, so in case of a normal closure
        // we can cancel the shut down hook and perform an early (synchronous)
        // shutdown of the python subprocess
        shutdownHook = new Thread(() -> {
            try {
//                System.out.println("Shut down hook called");
                _close();
//                System.out.println("Shut down hook finished");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        // This thread READS messages from the python subprocess
        Utils.startLoggedThread(() -> {
            final DataInputStream reader = new DataInputStream(process.getInputStream());
            startupLatch.countDown();
            out:
            while (running) {
                while (reader.available() < 4) if (!running) break out;
                int mid = reader.readInt();
                while (reader.available() < 4) if (!running) break out;
                int length = reader.readInt();
                while (reader.available() < length) if (!running) break out;
                final byte[] response = new byte[length];
                reader.readFully(response);
//                System.out.println("RESPONSE " + mid + " - " + new String(response, "UTF-8"));
                if (messageCallbacks.containsKey(mid))
                    messageCallbacks.remove(mid).complete(response);
            }
            shutdownLatch.countDown();
        });

        if (LOG_PYTHON_ERRORS) {
            // This thread OUTPUTS ERROR messages from the python subprocess
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
        }

        // The latch waits for all threads to startup
        startupLatch.await();
    }

    /**
     * Recursive function used to install jar python project
     *
     * @param jarDir     current dir **relative to jar file resources**
     * @param pyroot     python root project dir **relative to jar file resources**
     * @param installDir install directory, where the project is copied to **absolute system path**
     * @throws IOException
     */
    private static void copyJarDir(Path jarDir, Path pyroot, Path installDir) throws IOException {
        Files.list(jarDir).forEach(file -> {
            final String relpathFixed = pyroot.relativize(file).toString().replaceAll("^\\.\\.\\/gympy/", "");
            final Path dest = installDir.resolve(relpathFixed);
            try {
                if (Files.isDirectory(file)) {
                    Files.createDirectory(dest);
                    copyJarDir(file, pyroot, installDir);
                } else if (Files.isRegularFile(file)) {
                    Files.copy(Environment.class.getResourceAsStream(file.toString()), dest);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    int messageCount = 0;
    final Object incrementLock = new Object();
    final Object writerLock = new Object();

    /**
     * Accepts messages to be sent to the python subprocess
     *
     * @param msg to be transmitted
     */
    final void send(byte[] msg) throws IOException {
        send(0, msg);
    }

    /**
     * Accepts messages to be sent to the python subprocess
     *
     * @param mid
     * @param msg to be transmitted
     */
    final void send(int mid, byte[] msg) throws IOException {
        synchronized (writerLock) {
            writer.writeInt(mid);
            writer.writeInt(msg.length);
            writer.write(msg);
        }
        writer.flush();
    }

    final void send(byte[] msg, CompletableFuture<byte[]> future) throws IOException {
        int mid;
        synchronized (incrementLock) {
            mid = messageCount++;
        }
        send(mid, msg);
        messageCallbacks.put(mid, future);
    }

    /**
     * Relieves all resources used by this Gym
     *
     * @throws InterruptedException
     */
    @Override
    public void close() throws InterruptedException {
        // The shutdown hooks are removed
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
        _close();
    }

    private void _close() throws InterruptedException {
//        System.out.println("Close called");
        // running flag set to false, eventually shutting down all threads
        running = false;
        // This close method waits for the threads to finish
        shutdownLatch.await();
        synchronized (writerLock) {
            shutdownDestroySubprocess.run();
        }
        // The shutdown functions are then synchronously run
        if (pythonWasInstalled) shutdownUninstall.run();
//        System.out.println("Close finished");
    }

    //****************************************//
    //          UTIL FACTORY METHODS          //
    //****************************************//

    public static GymEnvironment make(String envId) throws InterruptedException, IOException, URISyntaxException {
        return new GymEnvironment(envId);
    }

    public static GymEnvironment make(String envId, boolean render) throws InterruptedException, IOException, URISyntaxException {
        return new GymEnvironment(envId, render);
    }

    public static Environment make(String envId, boolean render, Gym gym) throws IOException {
        return new Environment(envId, render, gym);
    }

    public static Environment make(String envId, Gym gym) throws IOException {
        return new Environment(envId, gym);
    }

    public static BoxBoxEnvironment makeBoxBox(String envId, Gym gym) throws IOException {
        return new BoxBoxEnvironment(envId, gym);
    }

    public static BoxDiscreteEnvironment makeBoxDiscrete(String envId, Gym gym) throws IOException {
        return new BoxDiscreteEnvironment(envId, gym);
    }


    public static DiscreteBoxEnvironment makeDiscreteBox(String envId, Gym gym) throws IOException {
        return new DiscreteBoxEnvironment(envId, gym);
    }

    public static DiscreteDiscreteEnvironment makeDiscreteDiscrete(String envId, Gym gym) throws IOException {
        return new DiscreteDiscreteEnvironment(envId, gym);
    }

    public BoxBoxEnvironment makeBoxBox(String envId) throws IOException {
        return makeBoxBox(envId, this);
    }

    public BoxDiscreteEnvironment makeBoxDiscrete(String envId) throws IOException {
        return makeBoxDiscrete(envId, this);
    }

    public DiscreteBoxEnvironment makeDiscreteBox(String envId) throws IOException {
        return makeDiscreteBox(envId, this);
    }

    public DiscreteDiscreteEnvironment makeDiscreteDiscrete(String envId) throws IOException {
        return makeDiscreteDiscrete(envId, this);
    }
}
