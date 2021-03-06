import datadog.trace.agent.test.log.injection.LogContextInjectionTestBase
import org.apache.logging.log4j.ThreadContext

class Log4jThreadContextTest extends LogContextInjectionTestBase {

  @Override
  def put(String key, Object value) {
    return ThreadContext.put(key, value as String)
  }

  @Override
  def get(String key) {
    return ThreadContext.get(key)
  }

  @Override
  def remove(String key) {
    return ThreadContext.remove(key)
  }

  @Override
  def clear() {
    return ThreadContext.clearAll()
  }

  @Override
  def getMap() {
    return ThreadContext.getImmutableContext()
  }
}
