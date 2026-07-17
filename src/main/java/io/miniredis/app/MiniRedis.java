package io.miniredis.app;

import io.miniredis.persistence.AOFManager;
import io.miniredis.store.DataStore;
import io.miniredis.store.ExpiryManager;
import io.miniredis.command.CommandHandler;

public class MiniRedis {
    private final DataStore dataStore;
    private final CommandHandler commandHandler;
    private final AOFManager aofManager;

    public MiniRedis(String aofPath) {
        this.dataStore = new DataStore();
        this.aofManager = new AOFManager(aofPath);
        this.commandHandler = new CommandHandler(dataStore, aofManager);
        aofManager.loadAOF(dataStore.getStore(), dataStore.getExpiry());
        Thread expiryThread = new Thread(new ExpiryManager(dataStore.getStore(), dataStore.getExpiry()));
        expiryThread.setDaemon(true);
        expiryThread.start();
    }

    public String execute(String command) {
        return commandHandler.handleCommand(command);
    }
}
