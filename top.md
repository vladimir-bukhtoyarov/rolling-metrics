# Top

*TODO:* complete documentation

## How to add Top to MetricRegistry?
The all of implementations of Top mentioned above do not implement of any MetricCore interface,
this decision was taken in order to provide ability to use Top without dependency from metrics-core library.
So you need to register Top manually as MetricSet in **MetricRegistry**, for example:
```java
   Top top = Top.builder(3).resetAllPositionsOnSnapshot().build();
   
   TimeUnit latencyOutputUnit = TimeUnit.MILLISECONDS;
   int digitsAfterDecimalPoint = 5;
   MetricSet metricSet = new TopMetricSet("my-top", top, latencyOutputUnit, digitsAfterDecimalPoint);
   registry.registerAll(metricSet);
```
