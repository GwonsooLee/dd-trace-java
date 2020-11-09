package datadog.trace.common.metrics;

import static datadog.trace.core.serialization.EncodingCachingStrategies.NO_CACHING;

import datadog.trace.core.serialization.ByteBufferConsumer;
import datadog.trace.core.serialization.WritableFormatter;
import datadog.trace.core.serialization.protobuf.ProtobufWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.BufferedSink;

@Slf4j
public class OkHttpReporter implements MetricReporter, ByteBufferConsumer {

  private static final MediaType PROTOBUF = MediaType.get("application/protobuf");

  private final CharSequence env;
  private final OkHttpClient client;
  private final HttpUrl metricsUrl;
  private final WritableFormatter writer;

  public OkHttpReporter(CharSequence env, String metricsUrl, OkHttpClient client) {
    this.env = env;
    this.client = client;
    this.metricsUrl = HttpUrl.get(metricsUrl);
    // TODO - it would probably better to allow buffer growth here,
    //  or even better, no buffer at all
    this.writer = new ProtobufWriter(null, ByteBuffer.allocate(1 << 20));
  }

  @Override
  public void start(int metricCount) {
    // TODO need clarification on what these two are
    writer.writeNull(); // start
    writer.writeNull(); // duration
    writer.startArray(metricCount);
  }

  @Override
  public void offer(MetricKey key, AggregateMetric aggregate) {
    writer.startStruct(12);
    writer.writeString(key.getOperationName(), NO_CACHING);
    writer.writeString(env, NO_CACHING);
    writer.writeString(key.getService(), NO_CACHING);
    writer.writeString(key.getResource(), NO_CACHING);
    writer.writeNull(); // version
    writer.writeNull(); // other tags - is HTTP status code supposed to go here?
    writer.writeDouble(aggregate.getHitCount());
    writer.writeDouble(aggregate.getErrorCount());
    writer.writeDouble(aggregate.getDuration());
    writer.writeNull(); // toplevel
    // TODO sketches go here when ready
    writer.writeNull(); // summary
    writer.writeNull(); // ErrSummary
  }

  @Override
  public void complete() {
    writer.flush();
  }

  @Override
  public void accept(int messageCount, ByteBuffer buffer) {
    try (final okhttp3.Response response =
        client
            .newCall(new Request.Builder().url(metricsUrl).put(new MetricsPayload(buffer)).build())
            .execute()) {
      log.debug("Metrics sent");
    } catch (IOException e) {
      log.debug("Failed to send metrics", e);
    }
  }

  // TODO rationalise duplication with DDAgentApi

  private static final class MetricsPayload extends RequestBody {

    private final ByteBuffer buffer;

    private MetricsPayload(ByteBuffer buffer) {
      this.buffer = buffer;
    }

    @Override
    public MediaType contentType() {
      return PROTOBUF;
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
      sink.write(buffer);
    }
  }
}
