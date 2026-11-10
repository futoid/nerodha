# ADR-001: Why Postgres, Redis and Kafka together

**Date:** 2026-11-10
**Status:** Accepted

## Problem
A paper-trading broker needs three different things from its data layer:
1. **Durable truth** for orders, trades, balances and positions. Nothing may be lost or double-counted after a crash.
2. **Fast, disposable state** for things like rate limits, cached prices and idempotency keys, where microseconds matter and loss is tolerable.
3. **An ordered, replayable event log** so the matching engine, risk checks and AI services can each consume the same order stream independently, and the order book can be rebuilt after a failure.

No single store is good at all three.

## Constraints
- Must run locally with Docker Compose, with no paid cloud services.
- One developer, so operational overhead is a real cost.
- Learning is an explicit goal: the stack should exercise real distributed-systems problems (ordering, idempotency, failure recovery), not just ship features.
- Per-symbol ordering must be guaranteed for the matching engine.

## Options considered

1. **Postgres only.** Orders in tables, a queue built from `SELECT ... FOR UPDATE SKIP LOCKED`, caching via materialized views.
   Main cost: polling-based queues add latency, there is no built-in consumer-group fan-out, and replaying history means extra bookkeeping.

2. **Postgres + Redis.** Redis for caching and rate limits, plus Redis Streams or pub/sub for events.
   Main cost: Redis is memory-bound, and its durability and replay guarantees are weaker than a log designed for retention. Pub/sub drops messages when a consumer is down.

3. **Postgres + Redis + Kafka.** Each tool owns one job.
   Main cost: three systems means three failure modes, more configuration, and eventual consistency between stores.

## Decision
Option 3, with clear ownership:

| Tool | Owns | Why it fits |
|---|---|---|
| Postgres | Source of truth: orders, trades, ledger, positions | ACID transactions, constraints, and a single place to reconcile against |
| Redis | Ephemeral state: rate limits, caches, idempotency keys | In-memory speed, atomic operations (Lua), and loss is acceptable |
| Kafka | Ordered event log: `orders`, `trades`, `market-ticks` | Per-key ordering by partition, consumer groups, and replay from any offset |

Kafka's per-partition ordering keyed by symbol is what tipped the decision. It lets the matching engine process each symbol's orders in sequence, and it lets the risk, AI and audit services read the same stream without interfering with each other.

## Trade-offs
- **Postgres alone would work for a paper-trading app.** Choosing three systems is partly a deliberate learning decision, and I accept the extra complexity for that reason.
- **Three failure modes to handle:** any of the three can be down or slow while the others are healthy.
- **Eventual consistency:** an order can be committed in Postgres before its Kafka event is published. This is why the outbox pattern is planned for Phase 2.
- **Parallelism is capped by partition count.** Observed on Day 4: a fourth consumer on a 3-partition topic sits idle. Adding partitions later changes key-to-partition hashing and can break ordering across the change.
- **Local ops burden:** more containers to keep healthy, more things to break on a cold start.

## How I'll know I'm wrong
- If by Phase 3 I never replay a topic, never use a second consumer group, and never rely on per-key ordering, Kafka was overkill and a Postgres-backed queue would have been enough.
- If Redis only ever holds data I could have kept in Postgres without a measurable slowdown, it isn't earning its place. The load test in Phase 6 should show the difference.
- If keeping the three systems consistent takes more effort than the engine itself, I over-engineered the data layer.
- Phase 3's exit test is the real check: `kill -9` the engine mid-load and recover with no lost or duplicate trades. If that is hard to achieve with this design, revisit it.