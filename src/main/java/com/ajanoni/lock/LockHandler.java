package com.ajanoni.lock;

import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.function.Supplier;

public interface LockHandler {

    <T> Uni<T> executeWithLock(List<String> names, Supplier<Uni<T>> supplier);

}
