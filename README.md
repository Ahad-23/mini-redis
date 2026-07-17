MiniRedis

Lightweight Redis-compatible in-memory key-value store in Java.

Quick Start

Build and run:

```
mvn clean test package
java -cp target/mini-redis-1.0.0.jar io.miniredis.app.Main
```

Connect via telnet:

```
telnet localhost 6379
SET mykey myvalue
GET mykey
```

Supported Commands

- SET key value [EX seconds] - Store string with optional TTL
- GET key - Retrieve string value
- DEL key - Delete key
- EXISTS key - Check key existence
- INCR key - Increment numeric value
- DECR key - Decrement numeric value
- FLUSHALL - Clear all data

Configuration

Override defaults via system properties:

```
java -Dredis.port=7000 -Dredis.maxClients=50 \
     -Dredis.aofPath=data/appendonly.aof \
     -cp target/mini-redis-1.0.0.jar io.miniredis.app.Main
```

Defaults in `src/main/resources/application.properties`:

```
redis.port=6379
redis.maxClients=100
redis.aofPath=data/appendonly.aof
```

Architecture

- Network: TCP server with fixed thread pool for concurrent clients
- Command: Space-separated token parser dispatching to handlers
- Storage: ConcurrentHashMap-based key-value store with expiry metadata
- Persistence: Append-Only File (AOF) logging with replay on startup

Project Structure

```
src/main/java/io/miniredis/
  app/      - Bootstrap and configuration
  server/   - TCP server and client handlers
  command/  - Command dispatcher
  store/    - In-memory data structures and expiry
  persistence/ - AOF file management
src/test/java/ - Unit tests
data/     - AOF file (created at runtime)
```

Features

- Thread-safe concurrent access via ConcurrentHashMap
- Key expiration with lazy cleanup
- Automatic AOF replay on startup
- Graceful shutdown on SIGTERM

Limitations

- String values only (no lists, sets, hashes)
- No transactions, pub/sub, or authentication
- No clustering or replication
- Single process; unbounded memory growth
- AOF file grows indefinitely (no compaction)
