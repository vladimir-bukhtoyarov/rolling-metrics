# Histograms
## Types of counters
The metrics core HDR provides three type of counters:
* [ResetAtSnapshotCounter](https://github.com/vladimir-bukhtoyarov/metrics-core-hdr/blob/1.3/src/main/java/com/github/metricscore/hdr/counter/ResetAtSnapshotCounter.java) - the counter which reset its state to zero after each invocation of *getSum()*.
* [ResetPeriodicallyCounter](https://github.com/vladimir-bukhtoyarov/metrics-core-hdr/blob/1.3/src/main/java/com/github/metricscore/hdr/counter/ResetPeriodicallyCounter.java) - the counter which reset its state to zero each time when configured interval is elapsed.
* [SmoothlyDecayingRollingCounter](https://github.com/vladimir-bukhtoyarov/metrics-core-hdr/blob/1.3/src/main/java/com/github/metricscore/hdr/counter/SmoothlyDecayingRollingCounter.java) the rolling time window counter which resets its state by chunks.

## ResetAtSnapshotCounter
TBD...

## ResetPeriodicallyCounter
TBD...

## SmoothlyDecayingRollingCounter
TBD...

## Integrate counters with MetricRegistry
TBD...

