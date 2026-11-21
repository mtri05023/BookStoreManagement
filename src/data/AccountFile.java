package data;

import utility.FileManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AccountFile {
    private final Path path;
    public AccountFile(Path path) { this.path = path; }
    public boolean exists() { return Files.exists(path); }
    public AccountRepository load() throws IOException {
        var lines = FileManager.readLines(path);
        AccountRepository repository = AccountTextCodec.decode(lines);
        boolean hasLegacyRecoveryField = lines.stream().skip(1).limit(Math.max(0, lines.size() - 2L))
                .anyMatch(line -> line.startsWith("ACCOUNT|") && line.split("\\|", -1).length == 9);
        if (hasLegacyRecoveryField) save(repository);
        return repository;
    }
    public void save(AccountRepository repository) throws IOException {
        if (Files.exists(path)) AccountTextCodec.decode(FileManager.readLines(path));
        FileManager.writeLinesAtomic(path, AccountTextCodec.encode(repository));
    }
    public Path getPath() { return path; }
}
