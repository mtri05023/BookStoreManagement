package utility;

import java.nio.file.Path;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public final class StoreConfig {
    public static final String DEFAULT_EMPLOYEE_ID = "NV001";
    public static final String STORE_NAME = "CỬA HÀNG SÁCH";
    public static final String FILE_VERSION = "1";
    public static final Charset TEXT_CHARSET = StandardCharsets.UTF_8;
    public static final Path DATA_DIRECTORY = Path.of("data");
    public static final Path STORE_FILE = DATA_DIRECTORY.resolve("store.txt");

    private StoreConfig() { }

    public static Path resolveStoreFile(String[] args) {
        if (args.length == 0) return STORE_FILE;
        if (args.length == 2 && args[0].equals("--data")) return Path.of(args[1]);
        throw new IllegalArgumentException("Cách chạy: presentation.Main [--data <file.txt>]");
    }
}
