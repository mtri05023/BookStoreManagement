package utility;

import java.nio.file.Path;

public final class StoreConfig {
    public static final String DEFAULT_EMPLOYEE_ID = "NV001";
    public static final Path DATA_DIRECTORY = Path.of("data");
    public static final Path STORE_FILE = DATA_DIRECTORY.resolve("store.txt");

    private StoreConfig() { }
}
