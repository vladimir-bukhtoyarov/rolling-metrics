# Hit-ratios
HitRatio the metric which measure ratio between hits and misses.

## Types of hit-ratios
The metrics core HDR provides four types of hit-ratio implementations:
* [ResetOnSnapshotHitRatio](https://github.com/vladimir-bukhtoyarov/metrics-core-hdr/blob/1.4/src/main/java/com/github/metricscore/hdr/hitratio/ResetOnSnapshotHitRatio.java) - the hit-ratio which reset its state to zero after each invocation of *getHitRatio()*.
* [ResetPeriodicallyHitRatio](https://github.com/vladimir-bukhtoyarov/metrics-core-hdr/blob/1.4/src/main/java/com/github/metricscore/hdr/hitratio/ResetPeriodicallyHitRatio.java) - the hit-ratio which reset its state to zero each time when configured interval is elapsed.
* [SmoothlyDecayingRollingHitRatio](https://github.com/vladimir-bukhtoyarov/metrics-core-hdr/blob/1.4/src/main/java/com/github/metricscore/hdr/hitratio/SmoothlyDecayingRollingHitRatio.java) The rolling time window hit-ratio implementation which resets its state by chunks.
* [UniformHitRatio](https://github.com/vladimir-bukhtoyarov/metrics-core-hdr/blob/1.4/src/main/java/com/github/metricscore/hdr/hitratio/UniformHitRatio.java) the hit-ratio which never evicts collected values.

## Concurrency properties for all implementations:
* Writing is lock-free.
* Ratio calculation is lock-free.

## ResetOnSnapshotHitRatio
The hit-ratio which reset its state to zero after each invocation of *getHitRatio()*.
 
Usage recommendations:
* When you do not need in "rolling time window" semantic. Else use *SmoothlyDecayingRollingHitRatio*
* When you need in 100 percents guarantee that one measure can not be reported twice.
* Only if one kind of reader interests in value of ratio. Usage of this implementation for case of multiple readers will be a bad idea because of readers will steal data from each other.

## ResetPeriodicallyHitRatio
The hit-ratio which reset its state to zero each time when configured interval is elapsed.

Usage recommendations:
* When you do not need in "rolling time window" semantic. Else use *SmoothlyDecayingRollingHitRatio*
* When you want to limit time which each update takes affect to ratio value in order to avoid reporting of obsolete measurements.
* Only if you accept the fact that several increments can be never observed by reader(because rotation to zero can happen before reader seen the written values).

## SmoothlyDecayingRollingHitRatio
The rolling time window hit-ratio implementation which resets its state by chunks.

The unique properties which makes this hit-ratio probably the best "rolling time window" implementation are following:
* Sufficient performance about tens of millions concurrent writes and reads per second.
* Predictable and low memory consumption, the memory which consumed by hit-ratio does not depend from amount and frequency of writes.
* Perfectly user experience, the continuous observation does not see the sudden changes of ratio. This property achieved by smoothly decaying of oldest chunk.

Usage recommendations:
* Only when you need in "rolling time window" semantic.
 
Performance considerations:
* You can consider writing speed as a constant. The write latency does not depend from count of chunk or frequency of chunk rotation.
* The writing depends only from level of contention between writers(internally hit-ratio implemented across ).
* The huge count of chunk leads to the slower calculation of their value. So precision of getHitRatio conflicts with latency of getHitRatio. You need to choose meaningful values. 
For example 10 chunks will guarantee at least 90% accuracy and ten million reads per second.

Example of usage:
```java
    // constructs the hit-ratio which divided by 10 chunks with 60 seconds time window.
    // one chunk will be reset to zero after each 6 second,
    HitRatio hitRatio = new SmoothlyDecayingRollingHitRatio(Duration.ofSeconds(60), 10);
    ...
    Something cached = cache.get(id);
    if (cached != null) {
        hitRatio.incrementHitCount();
    } else {
        hitRatio.incrementMissCount();
    }
```

## UniformHitRatio
The hit-ratio which never evicts collected values.

Usage recommendations:
* When you do not need in "rolling time window" semantic. Else use {@link SmoothlyDecayingRollingHitRatio}
* Normally you should not use this implementation because in real world use-cases you need to show measurements which actual to current moment of time or time window.


## How to add hit-ratio to MetricRegistry?
The all of types of hit-ratio mentioned above do not implement of any MetricCore interface,
this decision was taken in order to provide ability to use hit-ratio without dependency from metrics-core library.
So you need to register hit-ratio as Gauge in **MetricRegistry**, for example:
```java
   HitRatio ratio = new ResetOnSnapshotHitRatio();
   registry.register("my-hit-ratio", (Gauge<Double>) ratio::getHitRatio);
```
