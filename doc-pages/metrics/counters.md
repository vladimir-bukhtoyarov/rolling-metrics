# Counters
## Types of counters
The Rolling-Metrics provides three type of counters:
* [ResetOnSnapshotCounter](https://github.com/vladimir-bukhtoyarov/rolling-metrics/blob/2.0/src/main/java/com/github/rollingmetrics/counter/ResetOnSnapshotCounter.java) - the counter which reset its state to zero after each invocation of *getSum()*.
* [ResetPeriodicallyCounter](https://github.com/vladimir-bukhtoyarov/rolling-metrics/blob/2.0/src/main/java/com/github/rollingmetrics/counter/ResetPeriodicallyCounter.java) - the counter which reset its state to zero each time when configured interval is elapsed.
* [SmoothlyDecayingRollingCounter](https://github.com/vladimir-bukhtoyarov/rolling-metrics/blob/2.0/src/main/java/com/github/rollingmetrics/counter/SmoothlyDecayingRollingCounter.java) the rolling time window counter which resets its state by chunks.

## ResetOnSnapshotCounter
The counter which reset its state to zero after each invocation of *getSum()*.

Concurrency properties:
* Writing is lock-free. Writers do not block writers and readers.
* Sum reading always happen inside synchronized block, so readers block each other, but readers never block writers.
 
Usage recommendations:
* When you do not need in "rolling time window" semantic. Else use {@link SmoothlyDecayingRollingCounter}
* When you need in 100 percents guarantee that one measure can not be reported twice.
* Only if one kind of reader interests in value of counter. Usage of this implementation for case of multiple readers will be a bad idea because of readers will steal data from each other.

## ResetPeriodicallyCounter
The counter which reset its state to zero each time when configured interval is elapsed.

Concurrency properties:
* Writing is lock-free.
* Sum reading is lock-free.

Usage recommendations:
* When you do not need in "rolling time window" semantic. Else use *SmoothlyDecayingRollingCounter*
* When you want to limit time which each increment takes affect to counter sum in order to avoid reporting of obsolete measurements.
* Only if you accept the fact that several increments can be never observed by reader(because rotation to zero can happen before reader seen the written values).

## SmoothlyDecayingRollingCounter
The rolling time window counter implementation which resets its state by chunks.

The unique properties which makes this counter probably the best "rolling time window" implementation are following:
* Sufficient performance about tens of millions concurrent writes and reads per second.
* Predictable and low memory consumption, the memory which consumed by counter does not depend from amount and frequency of writes.
* Perfectly user experience, the continuous observation does not see the sudden changes of sum. This property achieved by smoothly decaying of oldest chunk of counter.
 
Concurrency properties:
* Writing is lock-free.
* Sum reading is lock-free.

Usage recommendations:
* Only when you need in "rolling time window" semantic.
 
Performance considerations:
* You can consider writing speed as a constant. The write latency does not depend from count of chunk or frequency of chunk rotation.
* The writing depends only from level of contention between writers(internally counter implemented across AtomicLong).
* The huge count of chunk leads to the slower calculation of their sum. So precision of sum conflicts with latency of sum. You need to choose meaningful values.
For example 10 chunks will guarantee at least 90% accuracy and ten million reads per second.

Example of usage:
```java
    // constructs the counter which divided by 10 chunks with 60 seconds time window.
    // one chunk will be reset to zero after each 6 second,
    WindowCounter counter = new SmoothlyDecayingRollingCounter(Duration.ofSeconds(60), 10);
    counter.add(42);
```