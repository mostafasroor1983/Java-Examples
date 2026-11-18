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

## Deeper dive
**Edge cases.** All backends unhealthy (return `Optional.empty()`, which the caller maps to 503); unregister during a pick (the snapshot
iteration of `CopyOnWriteArrayList` means a removed backend may still be returned once, so callers must handle connection failure);
counter overflow (`floorMod` keeps the index non-negative after `Integer.MAX_VALUE`).
**Fintech angle.** Payment gateways route across PSPs/acquirers for cost and approval rate, not only for load. That is a "strategy"
too (least-cost routing, failover on decline codes), and it must be deterministic and auditable for disputes.
**Production.** Health comes from active checks plus passive outlier detection (eject after N consecutive 5xx). Real load balancers
are infrastructure (Envoy, NGINX, cloud LB); in-process balancing is mainly client-side (gRPC).
