# Rolling-Metrics
The library contains collection of advanced metrics which missed in the original [Metrics Core](https://dropwizard.github.io/metrics/3.1.0/manual/core/) such as:
* Rolling time window counters. [See documentation for counters](counters.md).
* Rolling time window hit-ratio. [See documentation for hit-ratio](hit-ratio.md).
* Top of queries by latency. [See documentation for top](top.md).
* Loss-less capturing histograms(based on HdrHistogram). [See documentation for histograms](histograms.md).

## Build status
[![Coverage Status](https://coveralls.io/repos/github/vladimir-bukhtoyarov/rolling-metrics/badge.svg?branch=master)](https://coveralls.io/github/vladimir-bukhtoyarov/rolling-metrics?branch=master)
[![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Download](https://api.bintray.com/packages/vladimir-bukhtoyarov/maven/rolling-metrics/images/download.svg) ](https://bintray.com/vladimir-bukhtoyarov/maven/rolling-metrics/_latestVersion)
[![Join the chat at https://gitter.im/vladimir-bukhtoyarov/rolling-metrics](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/vladimir-bukhtoyarov/rolling-metrics?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## Get Rolling-Metrics library

#### By direct link
[Download compiled jar, sources, javadocs](https://github.com/vladimir-bukhtoyarov/rolling-metrics/releases/tag/2.0.1)

#### You can build Rolling Metrics from sources

```bash
git clone https://github.com/vladimir-bukhtoyarov/rolling-metrics.git
cd rolling-metrics
mvn clean install
```

#### You can add rolling-metrics to your project as maven dependency

The Rolling-Metrics library is distributed through [Bintray](http://bintray.com/), so you need to add Bintray repository to your `pom.xml`

```xml
     <repositories>
         <repository>
             <id>jcenter</id>
             <url>http://jcenter.bintray.com</url>
         </repository>
     </repositories>
```

Then include Rolling-Metrics as dependency to your `pom.xml`

```xml
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>rolling-metrics</artifactId>
    <version>2.0.1</version>
</dependency>
```

Have a question?
----------------
Feel free to ask in the [gitter chat](https://gitter.im/vladimir-bukhtoyarov/rolling-metrics)

License
-------
Copyright 2016 Vladimir Bukhtoyarov
Licensed under the Apache Software License, Version 2.0: <http://www.apache.org/licenses/LICENSE-2.0>.