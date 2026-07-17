Mini Redis
==========

Lightweight Redis-like in-memory store for demos and learning.

Quickstart
---------

Build and run with Maven:

```powershell
mvn -v
mvn test
mvn package
java -cp target/mini-redis-1.0.0.jar io.miniredis.app.Main
```

Configuration
-----------

`src/main/resources/application.properties` contains defaults. Override with system properties:

- `-Dredis.port=6379`
- `-Dredis.maxClients=100`
- `-Dredis.aofPath=data/appendonly.aof`

Project Layout
--------------

- `src/main/java/io/miniredis/app` - bootstrap and CLI/benchmark
- `src/main/java/io/miniredis/server` - networking/server
- `src/main/java/io/miniredis/command` - command dispatcher
- `src/main/java/io/miniredis/store` - in-memory store and expiry
- `src/main/java/io/miniredis/persistence` - AOF persistence
- `src/test/java` - unit tests

Notes
-----

AOF file is stored under `data/appendonly.aof` by default.
