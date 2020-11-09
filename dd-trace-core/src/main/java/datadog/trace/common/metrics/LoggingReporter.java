package datadog.trace.common.metrics;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingReporter implements MetricReporter {

  private final CharSequence env;
  private StringBuilder sb = new StringBuilder();
  int position = 0;

  public LoggingReporter(CharSequence env) {
    this.env = env;
  }

  @Override
  public void start(int metricCount) {
    sb.append('{').append("stats:[");
  }

  @Override
  public void offer(MetricKey key, AggregateMetric aggregate) {
    if (position++ > 0) {
      sb.append(',').append('\n');
    }
    sb.append('{')
        .append("name: ")
        .append(key.getOperationName())
        .append(", env: ")
        .append(env)
        .append(", service: ")
        .append(key.getService())
        .append(", resource: ")
        .append(key.getResource())
        .append(", hits: ")
        .append(aggregate.getHitCount())
        .append(", errors: ")
        .append(aggregate.getErrorCount())
        .append(", duration: ")
        .append(aggregate.getDuration())
        .append('}');
  }

  @Override
  public void complete() {
    sb.append('\n').append(']');
    log.info(sb.toString());
  }
}
