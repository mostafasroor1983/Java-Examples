# Approach 01 - Load Balancer (`com.revolut.buildit.loadbalancer`)

Requirements: register up to N backends (no duplicates), unregister, pick one per request, pluggable strategy, health awareness.

Design
- `LoadBalancer` interface; `SimpleLoadBalancer` with `CopyOnWriteArrayList` for the read-heavy backend list and a `ConcurrentHashMap` for health flags.
- `Strategy.pick(List<String> healthy)` keeps the algorithm separate: `RoundRobinStrategy` (atomic counter, `floorMod`), `RandomStrategy` (`ThreadLocalRandom`).
- `register/unregister` are `synchronized` so capacity and duplicate checks are atomic; `next()` runs lock-free on a snapshot.
- `Optional<String>` for "no healthy backend"; `IllegalStateException` for capacity (a programming/config error).

Trade-offs to discuss: weighted round robin, least-connections (needs completion callbacks), sticky sessions (consistent hashing),
health checks that flip flags asynchronously, and why round robin after unregister may skip one position.

Tests: duplicate/capacity/blank, round-robin order, unhealthy skipping, random covers all backends, 8 threads x 1000 picks are split exactly evenly.
