package com.ajanoni.lock;

import com.ajanoni.exception.LockAcquireException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import org.redisson.api.RPermitExpirableSemaphore;
import org.redisson.api.RedissonClient;

@ApplicationScoped
public class RedissonLockHandler implements LockHandler {

    private static final int LEASE_TIME_SECONDS = 90;
    private static final int ACQUIRE_WAIT_TIME_SECONDS = 3;
    private static final String LOCK_ACQUIRE_FAIL = "Unable to get the lock.";
    private static final String NOT_ACQUIRED = "NOT_ACQUIRED";

    private final RedissonClient redissonClient;

    public RedissonLockHandler(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public <T> Uni<T> executeWithLock(List<String> names, Supplier<Uni<T>> supplier) {
        return Uni.createFrom().item(() -> execute(names, supplier))
                .onItem().transformToUni(it -> it)
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    private <T> Uni<T> execute(List<String> names, Supplier<Uni<T>> supplier) {
        final Map<RPermitExpirableSemaphore, String> semaphores = getSemaphores(names, redissonClient);
        try {
            boolean notAllLocked = semaphores.values().stream().anyMatch(it -> it.equals(NOT_ACQUIRED));
            if (notAllLocked) {
                releaseSemaphores(semaphores);
                return Uni.createFrom().failure(() -> new LockAcquireException(LOCK_ACQUIRE_FAIL));
            }

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
                })
                .takeWhile(status -> !NOT_ACQUIRED.equals(status.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private String acquireSemaphore(RPermitExpirableSemaphore sem) {
        String retId = sem.tryAcquireAsync(ACQUIRE_WAIT_TIME_SECONDS, LEASE_TIME_SECONDS, TimeUnit.SECONDS).join();
        return Objects.isNull(retId) ? NOT_ACQUIRED : retId;
    }

    private void releaseSemaphores(Map<RPermitExpirableSemaphore, String> semaphoresMap) {
        semaphoresMap.forEach((semaphore, id) -> {
            if (!id.equals(NOT_ACQUIRED)) {
                semaphore.tryRelease(id);
            }
        });
    }
}
