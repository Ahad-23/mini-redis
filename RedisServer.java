import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RedisServer {
    private final int port;
    private final MiniRedis miniRedis;
    private final ExecutorService pool;
    private ServerSocket serverSocket;

    public RedisServer(int port, MiniRedis miniRedis, int maxClients) {
        this.port = port;
        this.miniRedis = miniRedis;
        this.pool = Executors.newFixedThreadPool(maxClients);
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("MiniRedis TCP server started on port " + port);
        while (!serverSocket.isClosed()) {
            Socket client = serverSocket.accept();
            pool.submit(new ClientHandler(client, miniRedis));
        }
    }

    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException ignored) {}
        pool.shutdownNow();
        System.out.println("MiniRedis TCP server stopped.");
    }
}
