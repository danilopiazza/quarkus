package io.quarkus.smallrye.faulttolerance.runtime;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.smallrye.faulttolerance.FaultToleranceOperationProvider;
import io.smallrye.faulttolerance.config.FaultToleranceMethods;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;

@Singleton
@Alternative
@Priority(1)
public class QuarkusFaultToleranceOperationProvider implements FaultToleranceOperationProvider {
    private static final Logger LOG = Logger.getLogger(QuarkusFaultToleranceOperationProvider.class);

    private final Map<CacheKey, FaultToleranceOperation> operationCache = new ConcurrentHashMap<>();
    private final Function<CacheKey, FaultToleranceOperation> cacheFunction = new Function<CacheKey, FaultToleranceOperation>() {
        @Override
        public FaultToleranceOperation apply(CacheKey key) {
            return createAtRuntime(key);
        }
    };

    /**
     * Called by SmallRyeFaultToleranceRecorder to init the operation cache.
     */
    void init(Map<CacheKey, FaultToleranceOperation> operationCache) {
        this.operationCache.putAll(operationCache);
    }

    @Override
    public FaultToleranceOperation get(Class<?> beanClass, Method method) {
        CacheKey key = new CacheKey(beanClass, method);
        FaultToleranceOperation existing = operationCache.get(key);
        return existing != null ? existing : operationCache.computeIfAbsent(key, cacheFunction);
    }

    private FaultToleranceOperation createAtRuntime(CacheKey key) {
        LOG.debugf("FaultToleranceOperation not found in the cache for %s creating it at runtime", key);
        return new FaultToleranceOperation(FaultToleranceMethods.create(key.beanClass, key.method));
    }

    public Map<CacheKey, FaultToleranceOperation> getOperationCache() {
        return operationCache;
    }

    static class CacheKey {
        private final Class<?> beanClass;
        private final Method method;
        private final int hashCode;

        public CacheKey(Class<?> beanClass, Method method) {
            this.beanClass = beanClass;
            this.method = method;
            this.hashCode = Objects.hash(beanClass, method);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CacheKey cacheKey = (CacheKey) o;
            return Objects.equals(beanClass, cacheKey.beanClass)
                    && Objects.equals(method, cacheKey.method);
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }
    }
}
