package com.ajanoni.redis;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import redis.embedded.RedisServer;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

@ApplicationScoped
public class RedisEmbeddedServer {

    private final RedisServer redisServer;

    public RedisEmbeddedServer() {
        redisServer = new RedisServer(6379); //TODO make config
    }

    void onStart(@Observes StartupEvent ev) {
        redisServer.start();
    }

    void onStop(@Observes ShutdownEvent ev) {
        redisServer.stop();
    }

}
