package com.ajanoni.lock.redis;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;
import redis.embedded.RedisCluster;
import redis.embedded.util.JedisUtil;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import org.jboss.logging.Logger;

@Startup
@ApplicationScoped
public class RedisEmbeddedServer {

    private static final Logger LOG = Logger.getLogger(RedisEmbeddedServer.class);

    private static final AtomicBoolean SERVER_STARTED = new AtomicBoolean(false);

    private final RedisCluster redisCluster;

    @Inject
    public RedisEmbeddedServer(RedisCluster redisCluster) {
        this.redisCluster = redisCluster;
    }

    void onStart(@Observes StartupEvent event) {
        if (redisCluster != null && !SERVER_STARTED.get()) {
            redisCluster.start();
            JedisUtil.sentinelHosts(redisCluster).forEach(LOG::info);
            SERVER_STARTED.set(true);
            LOG.info("Embedded Redis server started.");
        }
    }

    void onStop(@Observes ShutdownEvent event) {
        if (redisCluster != null && SERVER_STARTED.get()) {
            redisCluster.stop();
            LOG.info("Embedded Redis server stopped.");
        }
    }
}
