# Integration with Dropwizard Metrics
blah-blah

## Counters
### Why I need to use Counters provided by Rolling-Metrics if Dropwizard already supports funtionality for counters?
blah-blah
### How to add counters to MetricRegistry?
The all three types of counter mentioned above do not implement of any MetricCore interface,
this decision was taken in order to provide ability to use counters without dependency from metrics-core library.
So you need to register counter as Gauge in **MetricRegistry**, for example:
```java
   WindowCounter counter = new SmoothlyDecayingRollingCounter(Duration.ofSeconds(60), 10);
   registry.register("my-counter", (Gauge<Long>) counter::getSum);
```

## Hit-ratios
### Why I need to use hit-ratio provided by Rolling-Metrics if Dropwizard already provides funtionality for hit-ratio?
blah-blah
## How to add hit-ratio to MetricRegistry?
The all of types of hit-ratio mentioned above do not implement of any MetricCore interface,
this decision was taken in order to provide ability to use hit-ratio without dependency from metrics-core library.
So you need to register hit-ratio as Gauge in **MetricRegistry**, for example:
```java
   HitRatio ratio = new ResetOnSnapshotHitRatio();
   registry.register("my-hit-ratio", (Gauge<Double>) ratio::getHitRatio);
```

## Top
### How to add Top to MetricRegistry?
The implementation of Top does not implement of any MetricCore interface, this decision was taken in order to provide ability to use Top without dependency from metrics-core library.
So you need to register Top manually as MetricSet in **MetricRegistry**, for example:
```java
   Top top = Top.builder(3).resetAllPositionsOnSnapshot().build();

   TimeUnit latencyOutputUnit = TimeUnit.MILLISECONDS;
   int digitsAfterDecimalPoint = 5;
   MetricSet metricSet = new TopMetricSet("my-top", top, latencyOutputUnit, digitsAfterDecimalPoint);
   registry.registerAll(metricSet);
```

## Histograms
### Why I need to use Histograms provided by Rolling-Metrics if Dropwizard already supports funtionality for histograms?
Because of its sampling nature, all built-in Dorpwizard Metrics Core histogram implementations have the following problems:
* Can loss critical min/max values. See [this discussion](https://groups.google.com/forum/#!msg/mechanical-sympathy/I4JfZQ1GYi8/ocuzIyC3N9EJ) for more information.
* Can report obsolete values to snapshot. See [this blog post](http://taint.org/2014/01/16/145944a.html) for more information.

## How to Rolling-Metrics solves this problems?
The problems above already solved in scope of [HdrHistogram](https://github.com/HdrHistogram/HdrHistogram) project,
so this library just provides tightly integration of HdrHistogram into Metrics Core as first class citizen.
You should not waste your time to figure out who is better in the fight of metrics-core versus HdrHistogram, just use two solutions together.

From the HdrHistogram, you get:
* Min/max loss-less capturing.
* Non-blocking recording(short path optimization). It is not possible to come in situation when any writer is blocked by snapshot extraction.
* Garbage free recording. When reservoir increased up to its max size then it stops memory allocation and starts reusing already allocated memory, as result no garbage flows from new to old generation.

From the Metrics Core, you get:
* Useful, consistent, and extendable API. Metrics Core library is so popular because of its well-designed API.
* Integration(out of the box and third-parties) with hundreds libraries, servers, frameworks and API stacks.
* Integration(out of the box and third-parties) with many monitoring systems like graphite, ganglia, influxdb.

`Rolling-Metrics` provides ability to take advantages from both powerful libraries.

## Usage
### Metrics construction and registration
In order to construct metrics you need to create [builder](https://github.com/vladimir-bukhtoyarov/rolling-metrics/blob/master/src/main/java/com/github/rolling-metrics/histogram/HdrBuilder.java) instance.
The one builder instance can be reused to construct metrics multiple times.

#### Example of timer construction

```java
  HdrBuilder builder = new HdrBuilder();

  // Build and register timer in one line of code.
  // Prefer this style by default.
  Timer timer1 = builder.buildAndRegisterTimer(registry, "my-timer-1");

  // Build and register timer in two lines of code.
  // Use this style if you plan to register timer in multiple registers
  Timer timer2 = builder.buildTimer();
  registry1.register(timer2, "my-timer-2");

  // build and register timer in three lines of code(verbose way). Most likely you never need in this.
  Reservoir reservoir = builder.buildReservoir();
  Timer timer3 = new Timer(reservoir);
  registry.register(timer3, "my-timer-3");
```

#### Example of histogram construction
```java
  HdrBuilder builder = new HdrBuilder();

  // build and register histogram in one line of code (prefer this style)
  Histogram histogram1 = builder.buildAndRegisterHistogram(registry, "my-histogram-1");

  // Build and register timer in two lines of code.
  // Use this style if you plan to register histogram in multiple registers
  Histogram histogram2 = builder.buildHistogram();
  registry.register(histogram2, "my-histogram-2");

  // build and register histogram in three lines of code(verbose way). Most likely you never need in this.
  Reservoir reservoir = builder.buildReservoir();
  Histogram histogram3 = new Histogram(reservoir);
  registry.register(histogram3, "my-timer-3");
```