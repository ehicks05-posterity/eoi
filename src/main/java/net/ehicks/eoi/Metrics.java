package net.ehicks.eoi;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;

import java.util.LinkedHashMap;
import java.util.Map;

public class Metrics
{
    private static MetricRegistry metricRegistry = new MetricRegistry();

    public static MetricRegistry getMetricRegistry()
    {
        return metricRegistry;
    }

    /*
        <pool name>.pool.Wait
        A Timer instance collecting how long requesting threads to getConnection() are waiting for a connection (or timeout exception) from the pool.

        <pool name>.pool.Usage
        A Histogram instance collecting how long each connection is used before being returned to the pool. This is the "out of pool" or "in-use" time.

        <pool name>.pool.TotalConnections
        A CachedGauge, refreshed on demand at 1 second resolution, indicating the total number of connections in the pool.

        <pool name>.pool.IdleConnections
        A CachedGauge, refreshed on demand at 1 second resolution, indicating the number of idle connections in the pool.

        <pool name>.pool.ActiveConnections
        A CachedGauge, refreshed on demand at 1 second resolution, indicating the number of active (in-use) connections in the pool.

        <pool name>.pool.PendingConnections
        A CachedGauge, refreshed on demand at 1 second resolution, indicating the number of threads awaiting connections from the pool.
    */
    public static Map<String, String> getMetrics()
    {
        Map<String, String> metrics = new LinkedHashMap<>();

        MetricRegistry metricRegistry = ((MetricRegistry) EOI.cp.getMetricRegistry());
        Map<String, Metric> metricMap = metricRegistry.getMetrics();
        com.codahale.metrics.Timer wait = (com.codahale.metrics.Timer) metricMap.get(EOI.poolName + ".pool.Wait");
        Histogram usage = (Histogram) metricMap.get(EOI.poolName + ".pool.Usage");
        Gauge totalConnections = (Gauge) metricMap.get(EOI.poolName + ".pool.TotalConnections");
        Gauge idleConnections = (Gauge) metricMap.get(EOI.poolName + ".pool.IdleConnections");
        Gauge activeConnections = (Gauge) metricMap.get(EOI.poolName + ".pool.ActiveConnections");
        Gauge pendingConnections = (Gauge) metricMap.get(EOI.poolName + ".pool.PendingConnections");

        metrics.put("thread-wait-for-connection.meanRate", "" + String.format("%.2f", wait.getMeanRate()));
        metrics.put("thread-wait-for-connection.oneMinuteRate", "" + String.format("%.2f", wait.getOneMinuteRate()));
        metrics.put("total-values-recorded", "" + usage.getCount());
        metrics.put("connection-in-use-time.min", "" + usage.getSnapshot().getMin());
        metrics.put("connection-in-use-time.median", "" + usage.getSnapshot().getMedian());
        metrics.put("connection-in-use-time.max", "" + usage.getSnapshot().getMax());
        metrics.put("connection-in-use-time.95th", "" + usage.getSnapshot().get95thPercentile());
        metrics.put("connection-in-use-time.99th", "" + usage.getSnapshot().get99thPercentile());
        metrics.put("connection-in-use-time.999th", "" + usage.getSnapshot().get999thPercentile());
        metrics.put("totalConnections", "" + totalConnections.getValue());
        metrics.put("idleConnections", "" + idleConnections.getValue());
        metrics.put("activeConnections", "" + activeConnections.getValue());
        metrics.put("pendingConnections", "" + pendingConnections.getValue());

        return metrics;
    }
}
