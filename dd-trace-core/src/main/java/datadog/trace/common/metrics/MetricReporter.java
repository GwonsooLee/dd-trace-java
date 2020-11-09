package datadog.trace.common.metrics;

public interface MetricReporter {

  void start(int metricCount);

  void offer(MetricKey key, AggregateMetric aggregate);

  void complete();
}
