# Integration with Micrometer (http://micrometer.io/)

# Maven
```xml
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>rolling-metrics-micrometer</artifactId>
    <version>...</version>
</dependency>
```

# Spring-Boot
```java
@Bean
public MeterRegistry meterRegistry(){
    return new RollingMeterRegistry(
        DistributionStatisticConfig.builder()
            .expire(Duration.ofMinutes(window))
            .bufferLength(chunkCount)
            .build()
    )
}
```