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
        FileManager.writeLinesAtomic(path, StoreTextCodec.encode(Objects.requireNonNull(state)));
    }
}
