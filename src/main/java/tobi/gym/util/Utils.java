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

    private static final String GYM_PYTHON_FOLDER = "/tobi/gym/gym-py/";
    private static final String SHELL_PY_REL_PATH = "shell.py";
    static final String PYTHON_SHELL_PATH = GYM_PYTHON_FOLDER + SHELL_PY_REL_PATH;

    public static String installGym() throws URISyntaxException, IOException {
        URL location = Environment.class.getProtectionDomain().getCodeSource().getLocation();
        Path path = Paths.get(location.toURI());
        System.out.println("RUNNING FROM:" + location);
        System.out.println("Environment.class.getResource(PYTHON_SHELL_PATH) = " + Environment.class.getResource(PYTHON_SHELL_PATH));
        if (location.getPath().endsWith(".jar")) {
            try (FileSystem fs = FileSystems.newFileSystem(path, null)) {
                Path src = fs.getPath(GYM_PYTHON_FOLDER);
                final Path tempDir = Files.createTempDirectory("tobi.gym-");
                Files.walkFileTree(src, new Installer(tempDir, src));
                tempDir.toFile().deleteOnExit();
                return tempDir.resolve(SHELL_PY_REL_PATH).toString();
            }
        } else {
            return Environment.class.getResource(PYTHON_SHELL_PATH).getPath();
        }
    }


    public static final class Installer extends SimpleFileVisitor<Path> {

        private final Path target, source;

        private Installer(Path dst, Path src) {
            target = dst;
            source = src;
        }

        private Path resolve(Path path) {
            return target.resolve(source.relativize(path).toString());
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            Path dst = resolve(dir);
            Files.createDirectories(dst);
            return super.preVisitDirectory(dir, attrs);
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Path dst = resolve(file);
            Files.copy(Files.newInputStream(file), dst, StandardCopyOption.REPLACE_EXISTING);
            return super.visitFile(file, attrs);
        }
    }
}

