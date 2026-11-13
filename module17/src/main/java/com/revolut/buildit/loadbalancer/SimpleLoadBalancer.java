package com.revolut.buildit.loadbalancer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Bounded capacity, no duplicates, health flags, pluggable strategy.
 * Backend list is copy-on-write (read-heavy), health map is a CHM; next() works on a snapshot.
 */
public final class SimpleLoadBalancer implements LoadBalancer {
    private final int capacity;
    private final Strategy strategy;
    private final List<String> backends = new CopyOnWriteArrayList<>();
    private final Map<String, Boolean> healthy = new ConcurrentHashMap<>();

    public SimpleLoadBalancer(int capacity, Strategy strategy) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be positive");
        this.capacity = capacity;
        this.strategy = Objects.requireNonNull(strategy);
    }

    @Override
    public synchronized boolean register(String backend) {
        Objects.requireNonNull(backend);
        if (backend.isBlank()) throw new IllegalArgumentException("backend must not be blank");
        if (backends.contains(backend)) return false;
        if (backends.size() >= capacity) throw new IllegalStateException("capacity " + capacity + " reached");
        backends.add(backend);
        healthy.put(backend, true);
        return true;
    }

    @Override
    public synchronized boolean unregister(String backend) {
        healthy.remove(backend);
        return backends.remove(backend);
    }

    @Override
    public void markHealthy(String backend, boolean isHealthy) {
        healthy.computeIfPresent(backend, (k, v) -> isHealthy);
    }

    @Override
    public Optional<String> next() {
        List<String> candidates = new ArrayList<>();
        for (String b : backends) if (Boolean.TRUE.equals(healthy.get(b))) candidates.add(b);
        if (candidates.isEmpty()) return Optional.empty();
        return Optional.of(candidates.get(strategy.pick(candidates)));
    }

    @Override
    public List<String> backends() {
        return List.copyOf(backends);
    }
}
