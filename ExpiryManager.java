import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ExpiryManager implements Runnable {
    private final ConcurrentHashMap<String, String> store;
    private final ConcurrentHashMap<String, Long> expiry;
    private final Random random = new Random();
    private final int SAMPLE_SIZE = 20; // number of keys to check each run
    private final long cleanupIntervalMillis;

    public ExpiryManager(ConcurrentHashMap<String, String> store,
                         ConcurrentHashMap<String, Long> expiry) {
        this(store, expiry, 1000L); // default: run every 1 second
    }

    public ExpiryManager(ConcurrentHashMap<String, String> store,
                         ConcurrentHashMap<String, Long> expiry,
                         long cleanupIntervalMillis) {
        this.store = store;
        this.expiry = expiry;
        this.cleanupIntervalMillis = cleanupIntervalMillis;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                cleanupExpiredKeys();
                Thread.sleep(cleanupIntervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("ExpiryManager error: " + e.getMessage());
            }
        }
    }

    //Actively clean up expired keys by sampling a subset.
    private void cleanupExpiredKeys() {
        if (expiry.isEmpty()) return;

        long now = System.currentTimeMillis();
        List<String> keys = new ArrayList<>(expiry.keySet());
        for (int i = 0; i < Math.min(SAMPLE_SIZE, keys.size()); i++) {
            String key = keys.get(random.nextInt(keys.size()));
            Long expireAt = expiry.get(key);

            if (expireAt != null && expireAt <= now) {
                store.remove(key);
                expiry.remove(key);
                System.out.println("[EXPIRED] " + key);
            }
        }
    }

    // Lazy expiration: called by GET to ensure key is still valid.
    public String getWithExpiryCheck(String key) {
        Long expireAt = expiry.get(key);
        if (expireAt != null && expireAt <= System.currentTimeMillis()) {
            store.remove(key);
            expiry.remove(key);
            return null; // key expired
        }
        return store.get(key);
    }
}
