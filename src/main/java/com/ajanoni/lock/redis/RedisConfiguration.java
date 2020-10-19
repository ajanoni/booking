package com.ajanoni.lock.redis;

import com.ajanoni.lock.LockConfiguration;
import io.quarkus.arc.DefaultBean;
import io.quarkus.runtime.Startup;
import redis.embedded.RedisCluster;
import javax.enterprise.inject.Produces;

@Startup
public class RedisConfiguration {

    @Produces
    @DefaultBean
    public RedisCluster redisCluster(LockConfiguration lockConfig) {
        if (lockConfig.getRedisEmbedded().isStartServer()) {
            return RedisCluster.builder().sentinelPorts(lockConfig.getRedisEmbedded().getSentinelPorts()).quorumSize(1)
                    .ephemeralServers().replicationGroup(lockConfig.getRedisEmbedded().getMasterName(), 1)
                    .build();
        }

        return null;
    }
}
