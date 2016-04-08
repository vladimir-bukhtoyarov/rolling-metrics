# Metrics core HDR
This library is addressed to provide tightly integration of [HDR Histogram](https://github.com/HdrHistogram/HdrHistogram) into [Metrics Core](https://dropwizard.github.io/metrics/3.1.0/manual/core/) as first class citizen.

Do not waste your time to figure out who is better in the fight of metrics-core versus HdrHistorgam, just use two powerful solutions together.

## Build status
[![Build Status](https://travis-ci.org/vladimir-bukhtoyarov/metrics-core-hdr.svg?branch=master)](https://travis-ci.org/vladimir-bukhtoyarov/metrics-core-hdr)
[![Coverage Status](https://coveralls.io/repos/github/vladimir-bukhtoyarov/metrics-core-hdr/badge.svg?branch=master)](https://coveralls.io/github/vladimir-bukhtoyarov/metrics-core-hdr?branch=master)
[![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Download](https://api.bintray.com/packages/vladimir-bukhtoyarov/maven/metrics-core-hdr/images/download.svg) ](https://bintray.com/vladimir-bukhtoyarov/maven/metrics-core-hdr/_latestVersion)
[![Join the chat at https://gitter.im/vladimir-bukhtoyarov/metrics-core-hdr](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/vladimir-bukhtoyarov/metrics-core-hdr?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## Get Metrics-Core-HDR library

#### By direct link
[Download compiled jar, sources, javadocs](https://github.com/vladimir-bukhtoyarov/metrics-core-hdr/releases/tag/1.0.0)

#### You can build Metrics-Core-HDR from sources

```bash
git clone https://github.com/vladimir-bukhtoyarov/metrics-core-hdr.git
cd metrics-core-hdr
mvn clean install
```

#### You can add Metrics-Core-HDR to your project as maven dependency

The Metrics-Core-HDR library is distributed through [Bintray](http://bintray.com/), so you need to add Bintray repository to your `pom.xml`

```xml
     <repositories>
         <repository>
             <id>jcenter</id>
             <url>http://jcenter.bintray.com</url>
         </repository>
     </repositories>
```

Then include Metrics-Core-HDR as dependency to your `pom.xml`

```xml
<dependency>
    <groupId>com.github.metrics-core-addons</groupId>
    <artifactId>metrics-core-hdr</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Usage
### Metrics construction and registration
In order to construct metrics you need to create [builder](https://github.com/vladimir-bukhtoyarov/metrics-core-hdr/blob/master/src/main/java/com/github/metricscore/hdrhistogram/HdrBuilder.java) instance. 
The one builder instance can be reused to construct metrics multiple times. 

#### Example of timer construction

```java
  HdrBuilder builder = HdrBuilder();

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
  HdrBuilder builder = HdrBuilder();

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

## Basic configuration options
TODO
#### Number of significant value digits
TODO 
#### Lowest discernible value
TODO
#### Highest trackable value
TODO
#### Predefined percentiles
TODO

## Configuration settings for removing the old values of from reservoir.
TODO
#### Reset resevoir on snapshot
TODO
#### Reset reservoir periodically
TODO
#### Reset reservoir by chunks
TODO
#### Never reset
TODO

## Advanced features
TODO
#### Coordinated omission
TODO
#### Snapshot caching 
TODO
#### Get estimated footprint in bytes
TODO

Have a question?
----------------
Feel free to ask in the [gitter chat](https://gitter.im/vladimir-bukhtoyarov/metrics-core-hdr)

License
-------
Copyright 2016 Vladimir Bukhtoyarov
Licensed under the Apache Software License, Version 2.0: <http://www.apache.org/licenses/LICENSE-2.0>.