package io.miniredis.command;

import io.miniredis.store.DataStore;
import io.miniredis.persistence.AOFManager;

public class CommandHandler {
    private final DataStore dataStore;
    private final AOFManager aofManager;

    public CommandHandler(DataStore dataStore, AOFManager aofManager) {
        this.dataStore = dataStore;
        this.aofManager = aofManager;
    }

    public String handleCommand(String input) {
        if (input == null || input.trim().isEmpty()) return "(error) Empty command";
        String raw = input.trim();
        String[] tokens = raw.split("\\s+");
        String cmd = tokens[0].toUpperCase();
        try {
            switch (cmd) {
                case "SET":
                    return handleSet(raw, tokens);
                case "GET":
                    return handleGet(tokens);
                case "DEL":
                    return handleDel(tokens);
                case "EXISTS":
                    return handleExists(tokens);
                case "INCR":
                    return handleIncr(tokens);
                case "DECR":
                    return handleDecr(tokens);
                case "FLUSHALL":
                    return handleFlushAll();
                default:
                    return "(error) Unsupported command: " + tokens[0];
            }
        } catch (Exception ex) {
            return "(error) " + ex.getMessage();
        }
    }

    private String handleSet(String rawLine, String[] tokens) {
        if (tokens.length < 3) return "(error) SET requires key and value";
        String key = tokens[1];

        Integer ttlSeconds = null;
        String value;
        if (tokens.length >= 5 && tokens[tokens.length - 2].equalsIgnoreCase("EX")) {
            try {
                ttlSeconds = Integer.parseInt(tokens[tokens.length - 1]);
            } catch (NumberFormatException nfe) {
                return "(error) invalid EX seconds";
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < tokens.length - 2; i++) {
                if (i > 2) sb.append(' ');
                sb.append(tokens[i]);
            }
            value = sb.toString();
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < tokens.length; i++) {
                if (i > 2) sb.append(' ');
                sb.append(tokens[i]);
            }
            value = sb.toString();
        }

        dataStore.set(key, value);
        if (ttlSeconds != null) {
            long expireAt = System.currentTimeMillis() + ttlSeconds * 1000L;
            dataStore.setExpiry(key, expireAt);
        } else {
            dataStore.removeExpiry(key);
        }

        aofManager.appendCommand(rawLine);
        return "OK";
    }

    private String handleGet(String[] tokens) {
        if (tokens.length != 2) return "(error) GET requires key";
        String v = dataStore.get(tokens[1]);
        return v != null ? v : "(nil)";
    }

    private String handleDel(String[] tokens) {
        if (tokens.length != 2) return "(error) DEL requires key";
        boolean removed = dataStore.delete(tokens[1]);
        if (removed) aofManager.appendCommand("DEL " + tokens[1]);
        return removed ? "(integer) 1" : "(integer) 0";
    }

    private String handleExists(String[] tokens) {
        if (tokens.length != 2) return "(error) EXISTS requires key";
        boolean ex = dataStore.exists(tokens[1]);
        return ex ? "(integer) 1" : "(integer) 0";
    }

    private String handleIncr(String[] tokens) {
        if (tokens.length != 2) return "(error) INCR requires key";
        String key = tokens[1];
        try {
            long val = dataStore.incr(key);
            aofManager.appendCommand("INCR " + key);
            return String.valueOf(val);
        } catch (NumberFormatException nfe) {
            return "(error) ERR value is not an integer";
        }
    }

    private String handleDecr(String[] tokens) {
        if (tokens.length != 2) return "(error) DECR requires key";
        String key = tokens[1];
        try {
            long val = dataStore.decr(key);
            aofManager.appendCommand("DECR " + key);
            return String.valueOf(val);
        } catch (NumberFormatException nfe) {
            return "(error) ERR value is not an integer";
        }
    }

    private String handleFlushAll() {
        dataStore.getStore().clear();
        dataStore.getExpiry().clear();
        return "OK";
    }
}
