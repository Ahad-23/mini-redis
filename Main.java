/**
 * Start the MiniRedis TCP server (port 6379).
 */
public class Main {
    public static void main(String[] args) {
        final int port = 6379;
        final int maxClients = 100;
        MiniRedis miniRedis = new MiniRedis();
        RedisServer server = new RedisServer(port, miniRedis, maxClients);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown hook: stopping server...");
            server.stop();
        }));

        try {
            server.start();
        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            server.stop();
        }
    }
}
