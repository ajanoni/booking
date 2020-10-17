package com.ajanoni.service.lock;

import com.ajanoni.exception.LockAcquireException;
import io.smallrye.mutiny.Uni;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import org.redisson.Redisson;
import org.redisson.api.RPermitExpirableSemaphore;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

@ApplicationScoped
public class RedissonLockHandler implements LockHandler {

    private static final int LEASE_TIME = 60;
    private static final int ACQUIRE_WAIT_TIME = 60;
    private static final String LOCK_ACQUIRE_FAIL = "Unable to get the lock.";
    private static final String NOT_ACQUIRED = "NOT_ACQUIRED";

    public <T> Uni<T> executeWithLock(List<String> names, Supplier<Uni<T>> supplier) {
        System.out.println("START_TIME:" + Instant.now().toString());

        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://localhost:6379");

        RedissonClient redisson = Redisson.create(config);
        Map<RPermitExpirableSemaphore, String> semaphores = getSemaphores(names, redisson);
        try {
            boolean notAllLocked = semaphores.values().stream().anyMatch(it -> it.equals(NOT_ACQUIRED));
            if (notAllLocked) {
                releaseSemaphores(semaphores);
                return Uni.createFrom().failure(() -> new LockAcquireException(LOCK_ACQUIRE_FAIL));
            }
            System.out.println("MIDDLE_TIME:" + Instant.now().toString());
            return supplier.get()
                    .onTermination()
                    .invoke(() -> releaseSemaphores(semaphores));

        } catch (RuntimeException e) {
            releaseSemaphores(semaphores);
            return Uni.createFrom().failure(e);
        }
    }

    private Map<RPermitExpirableSemaphore, String> getSemaphores(List<String> names, RedissonClient redisson) {
        return names.stream()
                .map(name -> {
                    RPermitExpirableSemaphore semaphore = redisson.getPermitExpirableSemaphore(name);
                    semaphore.trySetPermits(1);
                    return Map.entry(semaphore, acquireSemaphore(semaphore));
                }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private String acquireSemaphore(RPermitExpirableSemaphore sem) {
        try {
            String id = sem.tryAcquire(ACQUIRE_WAIT_TIME, LEASE_TIME, TimeUnit.SECONDS);
            return Objects.isNull(id) ? NOT_ACQUIRED : id;
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        return NOT_ACQUIRED;
    }

    private void releaseSemaphores(Map<RPermitExpirableSemaphore, String> semaphoresMap) {
        semaphoresMap.forEach((semaphore, id) -> {
            if (!id.equals(NOT_ACQUIRED)) {
                semaphore.release(id);
            }
        });
        System.out.println("END_TIME:" + Instant.now().toString());
    }
}
