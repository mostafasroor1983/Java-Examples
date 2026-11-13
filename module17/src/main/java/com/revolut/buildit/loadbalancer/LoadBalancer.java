package com.revolut.buildit.loadbalancer;

import java.util.List;
import java.util.Optional;

/** Build It #1: register/unregister backends, pick one per request by strategy, respect health. */
public interface LoadBalancer {
    boolean register(String backend);

    boolean unregister(String backend);

    void markHealthy(String backend, boolean healthy);

    Optional<String> next();

    List<String> backends();
}
