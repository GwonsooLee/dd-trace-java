package datadog.trace.common.metrics;

public class AggregateMetric {
  private int errorCount;
  private int hitCount;
  private long duration;

  public void addHits(int count) {
    hitCount += count;
  }

  public void addErrors(int count) {
    errorCount += count;
  }

  public void recordDurations(long errorMask, long... durations) {
    for (long d : durations) {
      duration += d;
    }
  }

  public int getErrorCount() {
    return errorCount;
  }

  public int getHitCount() {
    return hitCount;
  }

  public long getDuration() {
    return duration;
  }

  public void clear() {
    this.errorCount = 0;
    this.hitCount = 0;
  }
}
