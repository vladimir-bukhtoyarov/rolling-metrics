# Metrics core HDR
The library contains collection of advanced metrics which missed in the original [Metrics Core](https://dropwizard.github.io/metrics/3.1.0/manual/core/) such as:
* Rolling time window counters. [See documentation for counters](counters.md).
* Rolling time window hit-ratio. [See documentation for hit-ratio](hit-ratio.md).
* Top of queries by latency. [See documentation for top](top.md).
* Loss-less capturing histograms(based on HdrHistogram). [See documentation for histograms](histograms.md).

## Build status
[![Build Status](https://travis-ci.org/vladimir-bukhtoyarov/metrics-core-hdr.svg?branch=master)](https://travis-ci.org/vladimir-bukhtoyarov/metrics-core-hdr)
[![Coverage Status](https://coveralls.io/repos/github/vladimir-bukhtoyarov/metrics-core-hdr/badge.svg?branch=master)](https://coveralls.io/github/vladimir-bukhtoyarov/metrics-core-hdr?branch=master)
[![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Download](https://api.bintray.com/packages/vladimir-bukhtoyarov/maven/metrics-core-hdr/images/download.svg) ](https://bintray.com/vladimir-bukhtoyarov/maven/metrics-core-hdr/_latestVersion)
[![Join the chat at https://gitter.im/vladimir-bukhtoyarov/metrics-core-hdr](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/vladimir-bukhtoyarov/metrics-core-hdr?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## Get Metrics-Core-HDR library

#### By direct link
[Download compiled jar, sources, javadocs](https://github.com/vladimir-bukhtoyarov/metrics-core-hdr/releases/tag/1.6.0)

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
    <version>1.6.0</version>
</dependency>
```

Have a question?
----------------
Feel free to ask in the [gitter chat](https://gitter.im/vladimir-bukhtoyarov/metrics-core-hdr)

License
-------
Copyright 2016 Vladimir Bukhtoyarov
Licensed under the Apache Software License, Version 2.0: <http://www.apache.org/licenses/LICENSE-2.0>.