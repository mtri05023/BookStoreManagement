package utility;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;

/** UTF-8 line I/O; record schemas belong to StoreTextCodec. */
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

    /** Fail safely when the filesystem cannot atomically replace the snapshot. */
    public static void writeLinesAtomic(Path path, List<String> lines) throws IOException {
        Objects.requireNonNull(lines, "Lines must not be null.");
        for (String line : lines) Objects.requireNonNull(line, "A line must not be null.");
        Path target = path.toAbsolutePath();
        Path parent = target.getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, ".bookstore-", ".tmp");
        try {
            Files.write(temporary, lines, StoreConfig.TEXT_CHARSET);
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
