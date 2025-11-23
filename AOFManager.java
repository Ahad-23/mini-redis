import java.io.*;
import java.util.Map;

/**
 * Simple Append-Only File manager with safe replay and rewrite.
 */
public class AOFManager {
    private final File aofFile;

    public AOFManager(String filename) {
        this.aofFile = new File(filename);
        try {
            if (!aofFile.exists()) aofFile.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create AOF file: " + e.getMessage(), e);
        }
    }

    // Append a command (synchronized)
    public synchronized void appendCommand(String command) {
        try (FileWriter fw = new FileWriter(aofFile, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(command);
            bw.newLine();
            bw.flush();
        } catch (IOException e) {
            System.err.println("AOF append failed: " + e.getMessage());
        }
    }

    /**
     * Replay AOF into the provided store and expiry maps.
     * This is READ-ONLY with respect to AOF file (no appends during replay).
     * Supports: SET key value [EX seconds], DEL key
     */
    public void loadAOF(Map<String, String> store, Map<String, Long> expiry) {
        if (!aofFile.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(aofFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] tokens = line.split("\\s+");
                String cmd = tokens[0].toUpperCase();
                if ("SET".equals(cmd)) {
                    // handle SET key value [EX seconds]
                    if (tokens.length < 3) continue;
                    String key = tokens[1];
                    // reconstruct value (tokens[2..n]) except possible EX tail
                    String value = null;
                    Integer ttlSeconds = null;
                    if (tokens.length >= 5) {
                        // check if second-last token is EX
                        String maybeEX = tokens[tokens.length - 2];
                        if ("EX".equalsIgnoreCase(maybeEX)) {
                            try {
                                ttlSeconds = Integer.parseInt(tokens[tokens.length - 1]);
                                // value is tokens[2..tokens.length-3]
                                StringBuilder sb = new StringBuilder();
                                for (int i = 2; i < tokens.length - 2; i++) {
                                    if (i > 2) sb.append(' ');
                                    sb.append(tokens[i]);
                                }
                                value = sb.toString();
                            } catch (NumberFormatException nfe) {
                                // fallback: treat full remainder as value
                            }
                        }
                    }
                    if (value == null) {
                        // simple case: value is tokens[2..end]
                        StringBuilder sb = new StringBuilder();
                        for (int i = 2; i < tokens.length; i++) {
                            if (i > 2) sb.append(' ');
                            sb.append(tokens[i]);
                        }
                        value = sb.toString();
                    }
                    store.put(key, value);
                    if (ttlSeconds != null) {
                        expiry.put(key, System.currentTimeMillis() + ttlSeconds * 1000L);
                    } else {
                        expiry.remove(key);
                    }
                } else if ("DEL".equals(cmd)) {
                    if (tokens.length >= 2) {
                        String key = tokens[1];
                        store.remove(key);
                        expiry.remove(key);
                    }
                } else {
                    // ignore unknown commands on replay (INCR/DECR may have been logged as 'INCR key')
                    if ("INCR".equals(cmd) || "DECR".equals(cmd)) {
                        // apply INCR/DECR semantics (best-effort)
                        if (tokens.length >= 2) {
                            String key = tokens[1];
                            String cur = store.get(key);
                            try {
                                long curv = cur == null ? 0L : Long.parseLong(cur);
                                if ("INCR".equals(cmd)) curv += 1;
                                else curv -= 1;
                                store.put(key, String.valueOf(curv));
                            } catch (NumberFormatException ignored) {
                                // skip invalid numeric during replay
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load AOF: " + e.getMessage());
        }
    }

    /**
     * Rewrite/compact AOF: write one SET per existing key (skip expired).
     */
    public synchronized void rewriteAOF(Map<String, String> storeMap, Map<String, Long> expiryMap) {
        File tmp = new File(aofFile.getAbsolutePath() + ".tmp");
        try (FileWriter fw = new FileWriter(tmp, false);
             BufferedWriter bw = new BufferedWriter(fw)) {
            long now = System.currentTimeMillis();
            for (Map.Entry<String, String> e : storeMap.entrySet()) {
                String key = e.getKey();
                Long exp = expiryMap.get(key);
                if (exp != null && exp <= now) continue;
                String value = e.getValue();
                // Escape newlines are not supported — keep simple
                bw.write("SET " + key + " " + value);
                bw.newLine();
            }
            bw.flush();
        } catch (IOException ex) {
            System.err.println("AOF rewrite failed: " + ex.getMessage());
            if (tmp.exists()) tmp.delete();
            return;
        }

        // Replace atomically if possible
        if (!tmp.renameTo(aofFile)) {
            // fallback on Windows
            try {
                if (aofFile.delete()) {
                    tmp.renameTo(aofFile);
                } else {
                    System.err.println("AOF rewrite: unable to replace file");
                }
            } catch (Exception ex) {
                System.err.println("AOF rewrite replace error: " + ex.getMessage());
            }
        }
    }
}
