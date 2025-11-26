import java.io.*;
import java.util.Map;

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
}
