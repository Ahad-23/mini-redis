public class CommandHandler {
    private final DataStore dataStore;

    public CommandHandler(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public String handleCommand(String input) {
        String[] parts = input.trim().split("\\s+");
        if (parts.length == 0) return "(error) Empty command";

        String command = parts[0].toUpperCase();

        switch (command) {
            case "SET":
                if (parts.length < 3) return "(error) SET key value [EX seconds]";
                String key = parts[1];
                String value = parts[2];
                dataStore.getStore().put(key, value);

                if (parts.length == 5 && parts[3].equalsIgnoreCase("EX")) {
                    long expiryTime = System.currentTimeMillis() + (Long.parseLong(parts[4]) * 1000);
                    dataStore.getExpiry().put(key, expiryTime);
                }
                return "OK";

            case "GET":
                key = parts[1];
                if (dataStore.getExpiry().containsKey(key) &&
                        dataStore.getExpiry().get(key) <= System.currentTimeMillis()) {
                    dataStore.getStore().remove(key);
                    dataStore.getExpiry().remove(key);
                    return "(nil)";
                }
                return dataStore.getStore().getOrDefault(key, "(nil)");

            case "DEL":
                key = parts[1];
                dataStore.getStore().remove(key);
                dataStore.getExpiry().remove(key);
                return "(integer) 1";

            default:
                return "(error) Unsupported command: " + command;
        }
    }
}
