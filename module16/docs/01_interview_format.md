# 01 - Revolut backend interview format (what to expect)

Typical loop for senior/staff backend (Java):
1. **Recruiter + take-home or screening** - sometimes a small coding task (money transfer API is a classic).
2. **Live coding (60-90 min)** - one problem from the "bank / transfer / payment / rate limiter" family. You share your screen in your own IDE; tests are expected, not optional.
3. **Java deep dive** - collections, concurrency, JMM, GC, JVM tuning, modern Java. Expect follow-ups until you reach the edge of your knowledge.
4. **System design** - payments, ledger, idempotency, exactly-once, outbox, sharding, multi-region.
5. **Behavioural / values** - ownership, "get it done", "deliver wow", "never settle"; concrete stories with metrics.

How you are scored in live coding: correctness and invariants first, then tests, then code clarity, then concurrency awareness, then communication. Working code with a missing edge case beats a perfect design with no running tests.

Golden rules
- Restate the problem and list invariants before typing.
- Write the smallest end-to-end slice, run the test, then iterate.
- Say what you are skipping ("I would add idempotency keys next") so the interviewer can steer.
- Use `BigDecimal` for money, records for values, `Optional` for lookups, typed results for expected failures.
- Keep a mental list of concurrency hazards and name them as you avoid them.
