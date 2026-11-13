package com.revolut.buildit.loadbalancer;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class RoundRobinStrategy implements Strategy {
    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public int pick(List<String> healthy) {
        return Math.floorMod(counter.getAndIncrement(), healthy.size());
    }
}
