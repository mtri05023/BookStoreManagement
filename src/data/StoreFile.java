package data;

import utility.FileManager;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** One snapshot file prevents book stock and invoices being saved in separate commits. */
public class StoreFile {
    private final Path path;

    public StoreFile(Path path) { this.path = Objects.requireNonNull(path); }

    public Path getPath() { return path; }

    public StoreState load() throws IOException {
        return StoreTextCodec.decode(FileManager.readLines(path));
    }

    public void save(StoreState state) throws IOException {
        if (java.nio.file.Files.exists(path)) {
            var existing = FileManager.readLines(path);
            // Validate before replacing: never seed or overwrite an empty/corrupt snapshot.
            StoreTextCodec.decode(existing);
            if (!existing.get(0).equals("VERSION|" + utility.StoreConfig.FILE_VERSION)) {
                String version = existing.get(0).substring("VERSION|".length());
                Path backup = path.resolveSibling(path.getFileName() + ".v" + version + ".bak");
                if (java.nio.file.Files.exists(backup) && !java.util.Arrays.equals(
                        java.nio.file.Files.readAllBytes(path), java.nio.file.Files.readAllBytes(backup))) {
                    backup = path.resolveSibling(path.getFileName() + ".v" + version + "-" + java.util.UUID.randomUUID() + ".bak");
                }
                if (!java.nio.file.Files.exists(backup)) java.nio.file.Files.copy(path, backup);
            }
        }
        FileManager.writeLinesAtomic(path, StoreTextCodec.encode(Objects.requireNonNull(state)));
    }
}
