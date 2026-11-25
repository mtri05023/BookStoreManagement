package bookstore;

public interface Identifiable {
    String getId();

    static int indexOf(Identifiable[] items, int size, String id) {
        if (id == null || id.isBlank()) return -1;
        String key = id.trim();
        for (int i = 0; i < size; i++) {
            if (items[i].getId().equalsIgnoreCase(key)) return i;
        }
        return -1;
    }
}
