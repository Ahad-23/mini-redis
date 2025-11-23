public class MiniRedis {
    private final DataStore dataStore;
    private final CommandHandler commandHandler;
    private final AOFManager aofManager;

    public MiniRedis() {
        this.dataStore = new DataStore();
        this.aofManager = new AOFManager("appendonly.aof");
        this.commandHandler = new CommandHandler(dataStore, aofManager);

        // Replay AOF into memory (read-only)
        aofManager.loadAOF(dataStore.getStore(), dataStore.getExpiry());

        // Start expiry manager thread
        Thread expiryThread = new Thread(new ExpiryManager(dataStore.getStore(), dataStore.getExpiry()));
        expiryThread.setDaemon(true);
        expiryThread.start();
    }

    public String execute(String command) {
        return commandHandler.handleCommand(command);
    }
}
