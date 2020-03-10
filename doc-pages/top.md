# Top
Top - the ```N```-position collection for queries sorted by latency in descended order. Each position in the ranking has the latency and description.
The ranking always stores no more than ```N``` positions, the longer queries displace shorter queries when ranking reaches max size.
Reading the javadocs for [Top](https://github.com/vladimir-bukhtoyarov/rolling-metrics/blob/2.0/src/main/java/com/github/rollingmetrics/ranking/Top.java) and [TopBuilder](https://github.com/vladimir-bukhtoyarov/rolling-metrics/blob/2.0/src/main/java/com/github/rollingmetrics/ranking/TopBuilder.java) classes is quite enough to start with this functionality.
 
Concurrency properties:
* Writing is lock-free. Writers never blocked by readers or other writers.
* Snapshot extraction always performed under lock, so readers blocks each other.

## The real world usage example
Lets detect slow queries to Cassandra database. The detection should be implemented on the client driver level.

Common requirements
* Latency threshold - 30 milliseconds
* Positions count - 10
* Rolling time window - 90 second with 3 chunks.


The query description must contain:
* CQL query text
* The Cassandra host on which a request has been performed.
* Exception message in case of error 

### First step - implement com.datastax.driver.core.LatencyTracker
```java
 import com.codahale.metrics.MetricRegistry;
 import com.codahale.metrics.MetricSet;
 import com.datastax.driver.core.*;
 import com.github.rollingmetrics.top.Ranking.Top;
 import com.github.rollingmetrics.dropwizard.adapter.TopMetricSet;

 import java.time.Duration;
 import java.util.Date;
 import java.util.concurrent.TimeUnit;
 import java.util.function.Supplier;

 /**
  * Created by vladimir.bukhtoyarov on 11.11.2016.
  */
 public class LatencyTopTracker implements LatencyTracker {

     private final Top ranking;

     public LatencyTopTracker(MetricRegistry metricRegistry) {
         this.ranking = Top.builder(10) // 10 positions in the ranking
                 .withLatencyThreshold(Duration.ofMillis(25)) // do not care about queries wich shorter than 25ms
                 .resetPositionsPeriodicallyByChunks(Duration.ofSeconds(60), 3) // position recorded in the ranking will take effect 60-80 seconds
                 .build();

         TimeUnit latencyUnit = TimeUnit.MILLISECONDS; // output latency in milliseconds
         int digitsAfterDecimalPoint = 3; // round output to 3 digits(example 34.945)
         MetricSet topMetricSet = new TopMetricSet("cassandra-query-ranking", ranking, latencyUnit, digitsAfterDecimalPoint);
         metricRegistry.registerAll(topMetricSet);
     }

     @Override
     public void update(Host host, Statement statement, Exception exception, long newLatencyNanos) {
         long requestTimestamp = System.currentTimeMillis();
         Supplier<String> lazyQueryDescriptionSupplier = () -> {
             String requestDate = new Date(requestTimestamp).toString();
             return new StringBuilder(host.getAddress().getHostName())
                     .append(" ")
                     .append(requestDate)
                     .append(" ")
                     .append(getQueryString(statement))
                     .append(exception == null ? "" : " error=" + exception.getMessage())
                     .toString();
         };
         ranking.update(requestTimestamp, newLatencyNanos, TimeUnit.NANOSECONDS, lazyQueryDescriptionSupplier);
     }

     private static String getQueryString(Statement statement) {
         if (statement instanceof RegularStatement) {
             RegularStatement rs = (RegularStatement) statement;
             String query = rs.getQueryString();
             return query.trim();
         }

         if (statement instanceof BoundStatement) {
             return ((BoundStatement) statement).preparedStatement().getQueryString().trim();
         }

         if (statement instanceof BatchStatement) {
             BatchStatement batchStatement = (BatchStatement) statement;
             StringBuilder builder = new StringBuilder("(");
             for (Statement stmt : batchStatement.getStatements()) {
                 builder.append(getQueryString(stmt));
                 builder.append("; ");
             }

             builder.append(")");
             return builder.toString();
         }

         // Unknown types of statement
         // Call toString() as a last resort
         return statement.toString();
     }

     @Override
     public void onRegister(Cluster cluster) {
         // do nothing
     }

     @Override
     public void onUnregister(Cluster cluster) {
         // do nothing
     }

 }
```

### Second step - register latency listener in Cassandra driver on the client side
```java
...
MetricRegistry metricRegistry = ...
Cluster cluster = ...

LatencyTracker latencyTopTracker = new LatencyTopTracker(metricRegistry);
cluster.register(latencyTopTracker);

```

### Check results
Run application and check results. The concrete way to expose MetricRegistry is out of scope of this example, see [Dropwizard-Metrics d](http://metrics.dropwizard.io/3.1.0/manual/) documentation about reporters.
The output bellow was produced through [MetricServlet](http://metrics.dropwizard.io/3.1.0/manual/servlets/), feel free to use any other reporter from Metrics library, or do reporting by itself.
```json
  "gauges" : {
    ... 
    
    "cassandra-query-ranking.0.description" : {
      "value" : "192.168.35.87 Fri Nov 11 12:57:38 UTC 2016 select data from my_keyspace.my_table where event_filter in :event_filters"
    },
    "cassandra-query-ranking.0.latency" : {
      "value" : 43.085
    },
    "cassandra-query-ranking.1.description" : {
      "value" : "192.168.35.87 Fri Nov 11 12:57:38 UTC 2016 select data from my_keyspace.my_table where event_filter in :event_filters"
    },
    "cassandra-query-ranking.1.latency" : {
      "value" : 42.959
    },
    "cassandra-query-ranking.2.description" : {
      "value" : "192.168.35.87 Fri Nov 11 12:57:38 UTC 2016 select data from my_keyspace.my_table where event_filter in :event_filters"
    },
    "cassandra-query-ranking.2.latency" : {
      "value" : 36.989
    },
    "cassandra-query-ranking.3.description" : {
      "value" : "192.168.35.87 Fri Nov 11 12:57:53 UTC 2016 select data from my_keyspace.my_table where event_filter in :event_filters"
    },
    "cassandra-query-ranking.3.latency" : {
      "value" : 32.510
    },
    "cassandra-query-ranking.4.description" : {
      "value" : "192.168.35.82 Fri Nov 11 12:56:43 UTC 2016 (insert into my_keyspace.my_table (  event_filter,  id,  data ) values ( :event_filter, :id, :data )using ttl :expires_in; insert into my_keyspace.my_table (  event_filter,  id,  data ) values ( :event_filter, :id, :data )using ttl :expires_in; insert into my_keyspace.my_table (  event_filter,  id,  data ) values ( :event_filter, :id, :data )using ttl :expires_in; insert into my_keyspace.my_table (  event_filter,  id,  data ) values ( :event_filter, :id, :data )using ttl :expires_in; insert into my_keyspace.subscription_by_id (  id,  data,  data_versions,  extension_application_id,  device_token_application_id_transport) values ( :id, :data, :data_versions, :extension_application_id, :device_token_application_id_transport)using ttl :expires_in; )"
    },
    "cassandra-query-ranking.4.latency" : {
      "value" : 32.008
    },
    "cassandra-query-ranking.5.description" : {
      "value" : "192.168.35.87 Fri Nov 11 12:56:43 UTC 2016 (insert into my_keyspace.my_table (  event_filter,  id,  data ) values ( :event_filter, :id, :data )using ttl :expires_in; insert into my_keyspace.my_table (  event_filter,  id,  data ) values ( :event_filter, :id, :data )using ttl :expires_in; insert into my_keyspace.my_table (  event_filter,  id,  data ) values ( :event_filter, :id, :data )using ttl :expires_in; insert into my_keyspace.my_table (  event_filter,  id,  data ) values ( :event_filter, :id, :data )using ttl :expires_in; insert into my_keyspace.subscription_by_id (  id,  data,  data_versions,  extension_application_id,  device_token_application_id_transport) values ( :id, :data, :data_versions, :extension_application_id, :device_token_application_id_transport)using ttl :expires_in; )"
    },
    "cassandra-query-ranking.5.latency" : {
      "value" : 31.741
    },
    "cassandra-query-ranking.6.description" : {
      "value" : "192.168.35.183 Fri Nov 11 12:56:31 UTC 2016 select data from my_keyspace.my_table where id = :id limit 1"
    },
    "cassandra-query-ranking.6.latency" : {
      "value" : 31.519
    },
    "cassandra-query-ranking.7.description" : {
      "value" : "192.168.35.183 Fri Nov 11 12:57:53 UTC 2016 (insert into my_keyspace.my_table (  event_filter,  id,  data ) values ( :event_filter, :id, :data )using ttl :expires_in; insert into my_keyspace.my_table (  event_filter,  id,  data ) values ( :event_filter, :id, :data )using ttl :expires_in; insert into my_keyspace.my_table (  event_filter,  id,  data ) values ( :event_filter, :id, :data )using ttl :expires_in; insert into my_keyspace.my_table (  event_filter,  id,  data ) values ( :event_filter, :id, :data )using ttl :expires_in; insert into my_keyspace.subscription_by_id (  id,  data,  data_versions,  extension_application_id,  device_token_application_id_transport) values ( :id, :data, :data_versions, :extension_application_id, :device_token_application_id_transport)using ttl :expires_in; )"
    },
    "cassandra-query-ranking.7.latency" : {
      "value" : 30.836
    },
    "cassandra-query-ranking.8.description" : {
      "value" : "192.168.35.82 Fri Nov 11 12:57:28 UTC 2016 select data from my_keyspace.my_table where event_filter in :event_filters"
    },
    "cassandra-query-ranking.8.latency" : {
      "value" : 30.334
    },
    "cassandra-query-ranking.9.description" : {
      "value" : "192.168.35.183 Fri Nov 11 12:57:50 UTC 2016 select data from my_keyspace.my_table where id = :id limit 1"
    },
    "cassandra-query-ranking.9.latency" : {
      "value" : 29.910
    },
    "cassandra-query-ranking.latencyUnit" : {
      "value" : "MILLISECONDS"
    },
 
    ...
    
 }
```