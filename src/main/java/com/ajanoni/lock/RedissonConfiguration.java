package com.ajanoni.lock;

import io.quarkus.arc.DefaultBean;
import io.quarkus.runtime.annotations.RegisterForReflection;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import org.apache.commons.collections4.CollectionUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

@Dependent
@RegisterForReflection
public class RedissonConfiguration {

    private static final String MESSAGE_CONFIG_MISSING = "Redis configuration is missing";

    @Produces
    @DefaultBean
    public RedissonClient redissonClient(LockConfiguration lockConfig) {
        if (CollectionUtils.isEmpty(lockConfig.getServerList())) {
            throw new IllegalStateException(MESSAGE_CONFIG_MISSING);
        }

        Config config = new Config();
        config.useSentinelServers()
                .addSentinelAddress(lockConfig.getServerList().toArray(String[]::new))
                .setMasterName(lockConfig.getMaster());
        return Redisson.create(config);
    }
}
