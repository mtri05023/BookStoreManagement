package utility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;

/** UTF-8 line I/O; record schemas belong to StoreTextCodec. */
public final class FileManager {
    private FileManager() { }

    public static List<String> readLines(Path path) throws IOException {
        Objects.requireNonNull(path, "Path must not be null.");
        if (Files.notExists(path)) return List.of();
        if (Files.isDirectory(path)) {
            throw new IOException("Đường dẫn là thư mục, không phải file: " + path.toAbsolutePath());
        }
        if (!Files.isReadable(path)) {
            throw new IOException("Không có quyền đọc file: " + path.toAbsolutePath());
        }
        return Files.readAllLines(path, StoreConfig.TEXT_CHARSET);
    }

    public static void writeLines(Path path, List<String> lines) throws IOException {
        Objects.requireNonNull(path, "Path must not be null.");
        Objects.requireNonNull(lines, "Lines must not be null.");
        for (String line : lines) Objects.requireNonNull(line, "A line must not be null.");
        Path target = path.toAbsolutePath();
        Path parent = target.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.write(target, lines, StoreConfig.TEXT_CHARSET);
    }

    /** Fail safely when the filesystem cannot atomically replace the snapshot. */
    public static void writeLinesAtomic(Path path, List<String> lines) throws IOException {
        Objects.requireNonNull(path, "Path must not be null.");
        Objects.requireNonNull(lines, "Lines must not be null.");
        for (String line : lines) Objects.requireNonNull(line, "A line must not be null.");
        Path target = path.toAbsolutePath();
        Path parent = target.getParent();
        if (parent != null) Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, ".bookstore-", ".tmp");
        try {
            Files.write(temporary, lines, StoreConfig.TEXT_CHARSET);
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
