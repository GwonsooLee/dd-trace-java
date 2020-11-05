package datadog.trace.instrumentation.aerospike4;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.listener.RecordListener;
import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.context.TraceScope;

public final class TracingRecordListener extends AbstractTracingListener<RecordListener>
    implements RecordListener {

  public TracingRecordListener(final AgentScope clientScope, final RecordListener listener) {
    super(clientScope, listener);
  }

  @Override
  public void onSuccess(final Key key, final Record record) {
    super.onSuccess();

    if (listener != null) {
      try (final TraceScope scope = continuation.activate()) {
        listener.onSuccess(key, record);
      }
    }
  }

  @Override
  public void onFailure(final AerospikeException cause) {
    super.onFailure(cause);

    if (listener != null) {
      try (final TraceScope scope = continuation.activate()) {
        listener.onFailure(cause);
      }
    }
  }
}
