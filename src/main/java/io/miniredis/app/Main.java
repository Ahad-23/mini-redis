package io.miniredis.app;

import io.miniredis.server.RedisServer;

public class Main {
    public static void main(String[] args) {
        Config cfg = new Config();
        final int port = cfg.getPort();
        final int maxClients = cfg.getMaxClients();
        final String aofPath = cfg.getAofPath();

        MiniRedis miniRedis = new MiniRedis(aofPath);
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
