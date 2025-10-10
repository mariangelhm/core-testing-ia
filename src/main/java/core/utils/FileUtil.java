package core.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Utility to simplify file interactions.
 */
public final class FileUtil {

    private FileUtil() {
    }

    /**
     * Reads the entire file content using UTF-8 encoding.
     *
     * @param path file to read
     * @return file contents as a single string
     * @throws IllegalStateException when the file cannot be read
     */
    public static String readString(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read file " + path, e);
        }
    }

    /**
     * Reads all lines from the file using UTF-8 encoding.
     *
     * @param path file to read
     * @return immutable list of lines from the file
     * @throws IllegalStateException when the file cannot be read
     */
    public static List<String> readLines(Path path) {
        try {
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read file " + path, e);
        }
    }

    /**
     * Writes text to the file creating missing parent directories using UTF-8 encoding.
     *
     * @param path destination file
     * @param content text to write
     * @throws IllegalStateException when the file cannot be written
     */
    public static void writeString(Path path, String content) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write file " + path, e);
        }
    }
}
