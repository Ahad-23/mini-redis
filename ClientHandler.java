import java.io.*;
import java.net.Socket;

/**
 * Handles one client connection (line-based protocol).
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final MiniRedis miniRedis;

    public ClientHandler(Socket socket, MiniRedis miniRedis) {
        this.socket = socket;
        this.miniRedis = miniRedis;
    }

    @Override
    public void run() {
        String remote = socket.getRemoteSocketAddress().toString();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            out.write("MiniRedis connected. Type commands or EXIT to disconnect.\r\n");
            out.flush();

            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    out.write("(error) Empty command\r\n");
                    out.flush();
                    continue;
                }
                if (line.equalsIgnoreCase("EXIT")) {
                    out.write("OK\r\n");
                    out.flush();
                    break;
                }
                String response = miniRedis.execute(line);
                out.write(response + "\r\n");
                out.flush();
            }
        } catch (IOException e) {
            // client disconnected; optional log:
            // System.err.println("Client " + remote + " error: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
