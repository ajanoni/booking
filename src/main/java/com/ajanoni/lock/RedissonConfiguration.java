package com.ajanoni.lock;

import io.quarkus.arc.DefaultBean;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import org.apache.commons.collections4.CollectionUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

@Dependent
public class RedissonConfiguration {

    @Produces
    @DefaultBean
    public RedissonClient redissonClient(LockConfiguration lockConfig) {
        if (CollectionUtils.isEmpty(lockConfig.getServerList())) {
            throw new IllegalStateException("Redis configuration is missing");
        }

        Config config = new Config();
        config.useSentinelServers()
                .addSentinelAddress(lockConfig.getServerList().toArray(String[]::new))
                .setMasterName("master1");
        return Redisson.create(config);
    }
}
