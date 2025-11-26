import java.io.*;
import java.net.Socket;

public class BenchmarkClient {

    public static void main(String[] args) throws Exception {

        int totalOps = 50000;  // number of operations
        int threads = 4;       // number of parallel benchmark threads

        Thread[] workers = new Thread[threads];

        for (int t = 0; t < threads; t++) {
            workers[t] = new Thread(() -> runBenchmark(totalOps));
        }

        long start = System.currentTimeMillis();

        // Start all threads
        for (Thread th : workers) th.start();
        // Wait for them to finish
        for (Thread th : workers) th.join();

        long end = System.currentTimeMillis();

        long elapsedMs = end - start;
        double opsPerSec = (totalOps * threads) / (elapsedMs / 1000.0);

        System.out.println("---- Benchmark Results ----");
        System.out.println("Total operations: " + (totalOps * threads));
        System.out.println("Time taken: " + elapsedMs + " ms");
        System.out.println("Throughput: " + (long) opsPerSec + " ops/sec");
    }

    private static void runBenchmark(int ops) {
        try (Socket socket = new Socket("127.0.0.1", 6379);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            in.readLine();  // skip welcome message

            for (int i = 0; i < ops; i++) {
                out.write("SET key" + i + " " + i);
                out.write("\r\n");
                out.flush();
                in.readLine(); // read response
            }

        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}
