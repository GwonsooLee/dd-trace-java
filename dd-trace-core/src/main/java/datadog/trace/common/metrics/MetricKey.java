package datadog.trace.common.metrics;

import java.util.Objects;

public final class MetricKey {
  private final CharSequence resource;
  private final CharSequence service;
  private final CharSequence operationName;
  private final int httpStatusCode;

  public MetricKey(
      CharSequence resource, CharSequence service, CharSequence operationName, int httpStatusCode) {
    this.resource = resource;
    this.service = service;
    this.operationName = operationName;
    this.httpStatusCode = httpStatusCode;
  }

  public CharSequence getResource() {
    return resource;
  }

  public CharSequence getService() {
    return service;
  }

  public CharSequence getOperationName() {
    return operationName;
  }

  public int getHttpStatusCode() {
    return httpStatusCode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MetricKey metricKey = (MetricKey) o;
    return httpStatusCode == metricKey.httpStatusCode
        && resource.equals(metricKey.resource)
        && service.equals(metricKey.service)
        && operationName.equals(metricKey.operationName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(resource, service, operationName, httpStatusCode);
  }
}
