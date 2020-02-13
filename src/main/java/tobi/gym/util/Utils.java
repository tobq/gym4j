package tobi.gym.util;

import tobi.gym.Box;
import org.json.JSONArray;
import tobi.gym.Environment;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

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

    private static final String GYM_PYTHON_FOLDER = "/tobi/gym/gympy/";
    private static final String SHELL_PY_REL_PATH = "shell.py";
    static final String PYTHON_SHELL_PATH = GYM_PYTHON_FOLDER + SHELL_PY_REL_PATH;

    public static String installGym() throws URISyntaxException, IOException {
        URL location = Environment.class.getProtectionDomain().getCodeSource().getLocation();
        Path path = Paths.get(location.toURI());
        if (location.getPath().endsWith(".jar")) {
            try (FileSystem fs = FileSystems.newFileSystem(path, null)) {
                Path src = fs.getPath(GYM_PYTHON_FOLDER);
                final Path tempDir = Files.createTempDirectory("tobi.gym.gympy-");
                copyJarDir(src, src, tempDir);
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
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
                                // directory iteration failed
                                throw e;
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }));

                return tempDir.resolve(SHELL_PY_REL_PATH).toString();
            }
        } else {
            return Environment.class.getResource(PYTHON_SHELL_PATH).getPath();
        }
    }

    private static void copyJarDir(Path jarDir, Path pyroot, Path outDir) throws IOException {
        Files.list(jarDir).forEach(file -> {
            final String relpathFixed = pyroot.relativize(file).toString().replaceAll("^\\.\\.\\/gympy/", "");
            final Path dest = outDir.resolve(relpathFixed);
            try {
                if (Files.isDirectory(file)) {
                    Files.createDirectory(dest);
                    copyJarDir(file, pyroot, outDir);
                } else if (Files.isRegularFile(file)) {
                    Files.copy(Environment.class.getResourceAsStream(file.toString()), dest);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


}

