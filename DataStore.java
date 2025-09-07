import java.util.concurrent.ConcurrentHashMap;

public class DataStore {
    private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> expiry = new ConcurrentHashMap<>();

    public ConcurrentHashMap<String, String> getStore() {
        return store;
    }

    public ConcurrentHashMap<String, Long> getExpiry() {
        return expiry;
    }
}
