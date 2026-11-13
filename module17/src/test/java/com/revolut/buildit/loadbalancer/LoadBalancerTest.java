package com.revolut.buildit.loadbalancer;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

class LoadBalancerTest {

    @Test
    void registrationRulesAndRoundRobin() {
        SimpleLoadBalancer lb = new SimpleLoadBalancer(3, new RoundRobinStrategy());
        assertThat(lb.register("a")).isTrue();
        assertThat(lb.register("a")).as("duplicate").isFalse();
        assertThat(lb.register("b")).isTrue();
        assertThat(lb.register("c")).isTrue();
        assertThatThrownBy(() -> lb.register("d")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> lb.register(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThat(List.of(lb.next().get(), lb.next().get(), lb.next().get(), lb.next().get())).containsExactly("a", "b", "c", "a");
        assertThat(lb.unregister("b")).isTrue();
        assertThat(lb.unregister("b")).isFalse();
        assertThat(lb.backends()).containsExactly("a", "c");
    }

    @Test
    void unhealthyBackendsAreSkippedAndEmptyIsOptional() {
        SimpleLoadBalancer lb = new SimpleLoadBalancer(3, new RoundRobinStrategy());
        assertThat(lb.next()).isEmpty();
        lb.register("a");
        lb.register("b");
        lb.markHealthy("a", false);
        assertThat(lb.next()).contains("b");
        assertThat(lb.next()).contains("b");
        lb.markHealthy("b", false);
        assertThat(lb.next()).isEmpty();
        lb.markHealthy("a", true);
        assertThat(lb.next()).contains("a");
    }

    @Test
    void randomStrategyHitsEveryBackendAndRoundRobinIsEvenUnderConcurrency() throws Exception {
        SimpleLoadBalancer random = new SimpleLoadBalancer(3, new RandomStrategy());
        for (String b : List.of("a", "b", "c")) random.register(b);
        Map<String, Integer> hits = new HashMap<>();
        for (int i = 0; i < 3000; i++) hits.merge(random.next().get(), 1, Integer::sum);
        assertThat(hits).containsOnlyKeys("a", "b", "c");
        assertThat(hits.values()).allSatisfy(v -> assertThat(v).isGreaterThan(500));

        SimpleLoadBalancer rr = new SimpleLoadBalancer(4, new RoundRobinStrategy());
        for (String b : List.of("a", "b", "c", "d")) rr.register(b);
        Map<String, Integer> counts = new java.util.concurrent.ConcurrentHashMap<>();
        int threads = 8, per = 1000;
        CountDownLatch done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) new Thread(() -> { try { for (int i = 0; i < per; i++) counts.merge(rr.next().get(), 1, Integer::sum); } finally { done.countDown(); } }).start();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(counts.values()).allMatch(v -> v == threads * per / 4);
    }
}
