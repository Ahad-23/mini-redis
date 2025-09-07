import java.util.concurrent.ConcurrentHashMap;

public class ExpiryManager implements Runnable {
    private final ConcurrentHashMap<String, String> store;
    private final ConcurrentHashMap<String, Long> expiry;

    public ExpiryManager(ConcurrentHashMap<String, String> store,
                         ConcurrentHashMap<String, Long> expiry) {
        this.store = store;
        this.expiry = expiry;
    }

    @Override
    public void run() {
        while (true) {
            long now = System.currentTimeMillis();
            for (String key : expiry.keySet()) {
                if (expiry.get(key) <= now) {
                    store.remove(key);
                    expiry.remove(key);
                    System.out.println(" Key expired: " + key);
                }
            }
            try {
                Thread.sleep(1000); // cleanup every second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
