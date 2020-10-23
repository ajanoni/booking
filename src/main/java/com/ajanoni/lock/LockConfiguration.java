package com.ajanoni.lock;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.Collections;
import java.util.List;

@ConfigProperties(prefix = "lock")
@RegisterForReflection
public class LockConfiguration {

    private RedisEmbedded redisEmbedded;
    private List<String> serverList;
    private String master;

    public List<String> getServerList() {
        return Collections.unmodifiableList(serverList);
    }

    public void setServerList(List<String> serverList) {
        this.serverList = serverList;
    }

    public String getMaster() {
        return master;
    }

    public void setMaster(String master) {
        this.master = master;
    }

    public RedisEmbedded getRedisEmbedded() {
        return redisEmbedded;
    }

    public void setRedisEmbedded(RedisEmbedded redisEmbedded) {
        this.redisEmbedded = redisEmbedded;
    }

    public static class RedisEmbedded {
        private boolean startServer;
        private String masterName = "master1";
        private List<Integer> sentinelPorts = List.of(26739, 26912);

        public boolean isStartServer() {
            return startServer;
        }

        public void setStartServer(boolean startServer) {
            this.startServer = startServer;
        }

        public List<Integer> getSentinelPorts() {
            return sentinelPorts;
        }

        public void setSentinelPorts(List<Integer> sentinelPorts) {
            this.sentinelPorts = sentinelPorts;
        }

        public String getMasterName() {
            return masterName;
        }

        public void setMasterName(String masterName) {
            this.masterName = masterName;
        }
    }
}
