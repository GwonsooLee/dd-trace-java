package datadog.trace.common.metrics;

import static datadog.trace.util.AgentThreadFactory.AgentThread.METRICS_AGGREGATOR;
import static datadog.trace.util.AgentThreadFactory.newAgentThread;

import datadog.trace.core.DDSpanData;
import datadog.trace.core.util.LRUCache;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.jctools.queues.MpscArrayQueue;
import org.jctools.queues.MpscBlockingConsumerArrayQueue;

public final class MetricsAggregator implements AutoCloseable {

  private final Queue<Batch> batchPool;
  private final ConcurrentHashMap<MetricKey, Batch> pending = new ConcurrentHashMap<>();
  private final Thread thread;
  private final Queue<Batch> inbox;

  public MetricsAggregator(MetricReporter reporter, int maxAggregates, int queueSize) {
    this.inbox = new MpscBlockingConsumerArrayQueue<>(queueSize);
    this.batchPool = new MpscArrayQueue<>(maxAggregates);
    this.thread =
        newAgentThread(
            METRICS_AGGREGATOR,
            new Aggregator(
                reporter, inbox, batchPool, pending, maxAggregates, 5, TimeUnit.SECONDS));
  }

  public void start() {
    thread.start();
  }

  public void publish(List<? extends DDSpanData> trace) {
    for (DDSpanData span : trace) {
      if (span.isMeasured()) {
        publish(span);
      }
    }
  }

  private void publish(DDSpanData span) {
    boolean error = span.getError() > 0;
    long durationNanos = span.getDurationNano();
    MetricKey key =
        new MetricKey(span.getResourceName(), span.getServiceName(), span.getOperationName(), 0);
    Batch batch = pending.get(key);
    if (null != batch) {
      if (batch.add(error, durationNanos)) {
        // added to a pending batch, skip the queue
        return;
      }
    }
    batch = newBatch(key);
    batch.add(error, durationNanos);
    // overwrite the last one if present, it's already full
    pending.put(key, batch);
    inbox.offer(batch);
  }

  private Batch newBatch(MetricKey key) {
    Batch batch = batchPool.poll();
    return (null == batch ? new Batch() : batch).setKey(key);
  }

  @Override
  public void close() {
    this.thread.interrupt();
  }

  private static final class Aggregator implements Runnable {

    private final Queue<Batch> batchPool;
    private final Queue<Batch> inbox;
    private final LRUCache<MetricKey, AggregateMetric> aggregates;
    private final ConcurrentHashMap<MetricKey, Batch> pending;
    private final MetricReporter reporter;
    private final long reportingIntervalNanos;

    private long lastReportTime = -1;

    private Aggregator(
        MetricReporter reporter,
        Queue<Batch> batchPool,
        Queue<Batch> inbox,
        ConcurrentHashMap<MetricKey, Batch> pending,
        int maxAggregates,
        long reportingInterval,
        TimeUnit reportingIntervalTimeUnit) {
      this.reporter = reporter;
      this.batchPool = batchPool;
      this.inbox = inbox;
      this.aggregates = new LRUCache<>(maxAggregates, 0.75f, maxAggregates * 4 / 3);
      this.pending = pending;
      this.reportingIntervalNanos = reportingIntervalTimeUnit.toNanos(reportingInterval);
    }

    @Override
    public void run() {
      Thread currentThread = Thread.currentThread();
      while (!currentThread.isInterrupted()) {
        Batch batch = inbox.poll();
        if (null != batch) {
          MetricKey key = batch.getKey();
          // important that it is still *this* batch pending, must not remove otherwise
          pending.remove(key, batch);
          AggregateMetric aggregate = aggregates.get(key);
          if (null == aggregate) {
            aggregate = new AggregateMetric();
            aggregates.put(key, aggregate);
            aggregate.clear();
          }
          batch.contributeTo(aggregate);
          // return the batch for reuse
          batchPool.offer(batch);
          reportIfNecessary();
        }
      }
    }

    private void reportIfNecessary() {
      if (lastReportTime == -1) {
        lastReportTime = System.nanoTime();
      } else {
        long now = System.nanoTime();
        if (now - lastReportTime > reportingIntervalNanos) {
          report();
          lastReportTime = now;
        }
      }
    }

    private void report() {
      reporter.start(aggregates.size());
      for (Map.Entry<MetricKey, AggregateMetric> aggregate : aggregates.entrySet()) {
        reporter.offer(aggregate.getKey(), aggregate.getValue());
      }
      // note that this may do IO and block
      reporter.complete();
    }
  }
}
