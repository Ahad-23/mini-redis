import java.util.Collections;
import java.util.Map;
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

    public void set(String key, String value) {
        store.put(key, value);
        // Keep expiry as-is (if caller wants to set/clear expiry, do it explicitly)
    }

    public String get(String key) {
        Long exp = expiry.get(key);
        if (exp != null && System.currentTimeMillis() > exp) {
            // expired
            store.remove(key);
            expiry.remove(key);
            return null;
        }
        return store.get(key);
    }

    public boolean delete(String key) {
        String removed = store.remove(key);
        expiry.remove(key);
        return removed != null;
    }

    public boolean exists(String key) {
        Long exp = expiry.get(key);
        if (exp != null && System.currentTimeMillis() > exp) {
            store.remove(key);
            expiry.remove(key);
            return false;
        }
        return store.containsKey(key);
    }

    public void setExpiry(String key, long epochMillis) {
        expiry.put(key, epochMillis);
    }

    public void removeExpiry(String key) {
        expiry.remove(key);
    }

    /**
     * Atomic INCR/DECR using compute.
     * Returns new value or throws NumberFormatException if value cannot be parsed.
     */
    public long incrBy(String key, long delta) {
        // Use compute to atomically update (also handles non-existing)
        final long[] result = new long[1];
        store.compute(key, (k, v) -> {
            if (v == null) {
                result[0] = delta > 0 ? 1 : -1;
                return String.valueOf(result[0]);
            }
            try {
                long cur = Long.parseLong(v);
                long updated = cur + delta;
                result[0] = updated;
                return String.valueOf(updated);
            } catch (NumberFormatException ex) {
                // keep old value (no change) and signal by setting result to Long.MIN_VALUE
                result[0] = Long.MIN_VALUE;
                return v;
            }
        });

        if (result[0] == Long.MIN_VALUE) {
            throw new NumberFormatException("value is not an integer");
        }
        return result[0];
    }

    public long incr(String key) {
        return incrBy(key, 1);
    }

    public long decr(String key) {
        return incrBy(key, -1);
    }

    /** Return a snapshot copy of the store (used by AOF rewrite) */
    public Map<String, String> dumpAll() {
        return Collections.unmodifiableMap(new ConcurrentHashMap<>(store));
    }
}
