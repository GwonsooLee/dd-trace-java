package datadog.trace.common.metrics;

import java.util.Arrays;

public final class Batch {

  private MetricKey key;

  private int count = 0;
  private long errorMask;
  private final long[] durations = new long[64];

  public MetricKey getKey() {
    return key;
  }

  public Batch setKey(MetricKey key) {
    this.key = key;
    return this;
  }

  /**
   * Writer side (application threads)
   *
   * @param error
   * @param durationNanos
   * @return
   */
  public synchronized boolean add(boolean error, long durationNanos) {
    if (count >= 64 && null != key) {
      return false;
    }
    if (error) {
      errorMask |= (1L << count);
    }
    durations[count] = durationNanos;
    ++count;
    return true;
  }

  /**
   * Reader side
   *
   * @param aggregate
   */
  public synchronized void contributeTo(AggregateMetric aggregate) {
    aggregate.addErrors(Long.bitCount(errorMask));
    aggregate.addHits(count);
    aggregate.recordDurations(errorMask, durations);
    clear();
  }

  private void clear() {
    this.key = null;
    this.count = 0;
    this.errorMask = 0L;
    Arrays.fill(durations, 0L);
  }
}
