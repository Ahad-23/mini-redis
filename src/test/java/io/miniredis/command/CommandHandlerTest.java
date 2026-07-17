package io.miniredis.command;

import io.miniredis.persistence.AOFManager;
import io.miniredis.store.DataStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CommandHandlerTest {

    @Test
    public void testSetAndGet() {
        DataStore ds = new DataStore();
        AOFManager aof = mock(AOFManager.class);
        CommandHandler h = new CommandHandler(ds, aof);

        assertEquals("OK", h.handleCommand("SET foo bar"));
        assertEquals("bar", ds.get("foo"));
        verify(aof).appendCommand("SET foo bar");
    }

    @Test
    public void testDel() {
        DataStore ds = new DataStore();
        AOFManager aof = mock(AOFManager.class);
        CommandHandler h = new CommandHandler(ds, aof);

        h.handleCommand("SET key val");
        assertEquals("(integer) 1", h.handleCommand("DEL key"));
        assertEquals("(nil)", h.handleCommand("GET key"));
        verify(aof).appendCommand("DEL key");
    }

    @Test
    public void testIncrDecrNumeric() {
        DataStore ds = new DataStore();
        AOFManager aof = mock(AOFManager.class);
        CommandHandler h = new CommandHandler(ds, aof);

        h.handleCommand("SET cnt 5");
        assertEquals("6", h.handleCommand("INCR cnt"));
        assertEquals("5", h.handleCommand("DECR cnt"));
    }

    @Test
    public void testIncrNonNumeric() {
        DataStore ds = new DataStore();
        AOFManager aof = mock(AOFManager.class);
        CommandHandler h = new CommandHandler(ds, aof);

        h.handleCommand("SET x foo");
        assertTrue(h.handleCommand("INCR x").contains("ERR value is not an integer"));
    }

    @Test
    public void testExpiryEX() throws Exception {
        DataStore ds = new DataStore();
        AOFManager aof = mock(AOFManager.class);
        CommandHandler h = new CommandHandler(ds, aof);

        assertEquals("OK", h.handleCommand("SET t1 val EX 1"));
        Thread.sleep(1200);
        assertEquals("(nil)", h.handleCommand("GET t1"));
    }
}
