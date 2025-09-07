public class MiniRedis {
    private final DataStore dataStore;
    private final CommandHandler commandHandler;

    public MiniRedis() {
        this.dataStore = new DataStore();
        this.commandHandler = new CommandHandler(dataStore);

        // Start expiry manager thread
        Thread expiryThread = new Thread(new ExpiryManager(
                dataStore.getStore(),
                dataStore.getExpiry()
        ));
        expiryThread.setDaemon(true);
        expiryThread.start();
    }

    public String execute(String command) {
        return commandHandler.handleCommand(command);
    }
}
