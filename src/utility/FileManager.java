package utility;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** UTF-8 line I/O. Model serialization and persistence wiring remain module work. */
public final class FileManager {
    private FileManager() { }

    public static List<String> readLines(Path path) throws IOException {
        if (Files.notExists(path)) return List.of();
        return Files.readAllLines(path, StandardCharsets.UTF_8);
    }

    public static void writeLines(Path path, List<String> lines) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.write(path, lines, StandardCharsets.UTF_8);
    }
}
