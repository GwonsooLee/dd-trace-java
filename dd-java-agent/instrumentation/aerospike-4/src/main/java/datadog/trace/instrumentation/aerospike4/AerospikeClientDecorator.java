package datadog.trace.instrumentation.aerospike4;

import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.activateSpan;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.startSpan;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.cluster.Node;
import datadog.trace.api.DDSpanTypes;
import datadog.trace.api.DDTags;
import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.trace.bootstrap.instrumentation.api.UTF8BytesString;
import datadog.trace.bootstrap.instrumentation.decorator.DBTypeProcessingDatabaseClientDecorator;

public class AerospikeClientDecorator extends DBTypeProcessingDatabaseClientDecorator<Node> {
  public static final UTF8BytesString AEROSPIKE_JAVA =
      UTF8BytesString.createConstant("aerospike-java");

  public static final AerospikeClientDecorator DECORATE = new AerospikeClientDecorator();

  @Override
  protected String[] instrumentationNames() {
    return new String[] {"aerospike"};
  }

  @Override
  protected String service() {
    return "aerospike";
  }

  @Override
  protected CharSequence component() {
    return "java-aerospike";
  }

  @Override
  protected CharSequence spanType() {
    return DDSpanTypes.AEROSPIKE;
  }

  @Override
  protected String dbType() {
    return "aerospike";
  }

  @Override
  protected String dbUser(final Node node) {
    return null;
  }

  @Override
  protected String dbInstance(final Node node) {
    return null;
  }

  @Override
  protected String dbHostname(final Node node) {
    return null;
  }

  public void withMethod(final AgentSpan span, final String methodName) {
    span.setTag(DDTags.RESOURCE_NAME, spanNameForMethod(AerospikeClient.class, methodName));
  }

  public AgentScope startAerospikeSpan(final String methodName) {
    final AgentSpan span = startSpan(AEROSPIKE_JAVA);
    afterStart(span);
    withMethod(span, methodName);
    return activateSpan(span);
  }

  public void finishAerospikeSpan(final AgentScope scope, final Throwable error) {
    if (scope != null) {
      final AgentSpan span = scope.span();
      if (error != null) {
        onError(span, error);
      }
      beforeFinish(span);
      span.finish();
      scope.close();
    }
  }
}
