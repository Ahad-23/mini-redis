package io.miniredis.app;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    private final Properties props = new Properties();

    public Config() {
        try (InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("application.properties")) {
            if (is != null) props.load(is);
        } catch (IOException ignored) {}
    }

    public int getPort() {
        String v = System.getProperty("redis.port", props.getProperty("redis.port", "6379"));
        return Integer.parseInt(v);
    }

    public int getMaxClients() {
        String v = System.getProperty("redis.maxClients", props.getProperty("redis.maxClients", "100"));
        return Integer.parseInt(v);
    }

    public String getAofPath() {
        return System.getProperty("redis.aofPath", props.getProperty("redis.aofPath", "data/appendonly.aof"));
    }
}
