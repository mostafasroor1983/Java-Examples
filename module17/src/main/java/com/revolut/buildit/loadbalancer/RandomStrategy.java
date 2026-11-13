package com.revolut.buildit.loadbalancer;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class RandomStrategy implements Strategy {
    @Override
    public int pick(List<String> healthy) {
        return ThreadLocalRandom.current().nextInt(healthy.size());
    }
}
