# Rolling-Metrics
Rolling-Metrics is the monitoring library which provides advanced monitoring structures with rolling time window semantic.

## Supported monitoring primitives
* Rolling time window counters. [See documentation for counters](doc-pages/counters.md).
* Rolling time window hit-ratio. [See documentation for hit-ratio](doc-pages/hit-ratio.md).
* Ranking of queries by latency. [See documentation for ranking](doc-pages/ranking.md).
* Loss-less capturing histograms(based on HdrHistogram). [See documentation for histograms](doc-pages/histograms.md).

## Integrations
The reasons why anybody can decide to to use ```Rolling-metrics``` together with anything else:
* Any monitoring metric needs to be reported and visualized. In same time, Rolling-Metrics does not provide any functionality related to monitoring data reporting,
because author has no time to reinvent the wheel. Instead, you are free to integrate ```Rolling-metrics``` as first class citizen to another monitoring technology(for example JMX)
and reuse already existed reporters and visualizers created for this technology.
* You already use another monitoring library, but it does not obeys to monitoring manifest(described below), as result you are unhappy by latency, contention, precision, unpredictable retention, or unnecessary garbage generation.
* You already use another monitoring library, but some functionality provided by Rolling-Metrics is missed in library which you already use.

```Rolling-metrics``` seamlessly integrated with following monitoring frameworks and standarts:
* [JMX](http://www.oracle.com/technetwork/java/javase/tech/javamanagement-140525.html). See documentation about JMX integration [there](doc-pages/integration/jmx.md).
* [Eclipse MicroProfile Metrics](https://github.com/eclipse/microprofile-metrics). See documentation about Eclipse MicroProfile integration [there](doc-pages/integration/eclipse-microprofile.md).
* [Dropwizard Metrics](http://metrics.dropwizard.io), both ```3.x``` and ```4.x``` brachnes. See documentation about Dropwizard Metrics integration [there](doc-pages/integration/jmx.md).

## Manifest for the primary monitoring primitives
TODO https://github.com/vladimir-bukhtoyarov/rolling-metrics/issues/17


## Build status
[![Coverage Status](https://coveralls.io/repos/github/vladimir-bukhtoyarov/rolling-metrics/badge.svg?branch=master)](https://coveralls.io/github/vladimir-bukhtoyarov/rolling-metrics?branch=master)
[![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Download](https://api.bintray.com/packages/vladimir-bukhtoyarov/maven/rolling-metrics/images/download.svg) ](https://bintray.com/vladimir-bukhtoyarov/maven/rolling-metrics/_latestVersion)
[![Join the chat at https://gitter.im/vladimir-bukhtoyarov/rolling-metrics](https://badges.gitter.im/vladimir-bukhtoyarov/rolling-metrics.svg)](https://gitter.im/vladimir-bukhtoyarov/rolling-metrics?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## Get Rolling-Metrics library

#### You can add rolling-metrics to your project as maven dependency

The Rolling-Metrics library is distributed through both [JCenter](https://bintray.com/bintray/jcenter?filterByPkgName=rolling-metrics) and [Maven Central](http://search.maven.org/),
use any of them:
```xml
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>rolling-metrics</artifactId>
    <version>3.0.0</version>
</dependency>
```

#### You can build Rolling Metrics from sources

```bash
git clone https://github.com/vladimir-bukhtoyarov/rolling-metrics.git
cd rolling-metrics
mvn clean install
```

Have a question?
----------------
Feel free to ask in the [gitter chat](https://gitter.im/vladimir-bukhtoyarov/rolling-metrics)

License
-------
Copyright 2016 Vladimir Bukhtoyarov
Licensed under the Apache Software License, Version 2.0: <http://www.apache.org/licenses/LICENSE-2.0>.
