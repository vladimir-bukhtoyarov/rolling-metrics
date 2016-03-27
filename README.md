# Metrics core HDR
This library is addressed to provide tightly integration of [HDR Histogram](https://github.com/HdrHistogram/HdrHistogram) into [Metrics Core](https://dropwizard.github.io/metrics/3.1.0/manual/core/) as first class citizen.

Do not waste your time to figure out who is better in the fight of metrics-core versus HdrHistorgam, just use two powerful solutions together.

## Build status
[![Build Status](https://travis-ci.org/vladimir-bukhtoyarov/metrics-core-hdr.svg?branch=master)](https://travis-ci.org/vladimir-bukhtoyarov/metrics-core-hdr)
[![Coverage Status](https://coveralls.io/repos/github/vladimir-bukhtoyarov/metrics-core-hdr/badge.svg?branch=master)](https://coveralls.io/github/vladimir-bukhtoyarov/metrics-core-hdr?branch=master)

### Get Metrics-Core-HDR library

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

### Basic usage

#### Simple example

```java
  HdrBuilder builder = HdrBuilder();

  // build and register timer
  Timer timer1 = builder.buildAndRegisterTimer(registry, "my-timer-1");

  // build and register timer in another way
  Timer timer2 = builder.buildTimer();
  registry.register(timer2, "my-timer-2");

  // build and register histogram
  Histogram histogram1 = builder.buildAndRegisterHistogram(registry, "my-histogram-1");

  // build and register histogram in another way
  Histogram histogram2 = builder.buildHistogram();
  registry.register(histogram2, "my-histogram-2");

```
