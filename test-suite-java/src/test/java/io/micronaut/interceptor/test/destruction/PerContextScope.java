package io.micronaut.interceptor.test.destruction;

import io.micronaut.context.scope.AbstractConcurrentCustomScope;
import io.micronaut.context.scope.CreatedBean;
import io.micronaut.inject.BeanIdentifier;
import jakarta.inject.Singleton;

import java.util.HashMap;
import java.util.Map;

/** The scope of {@link PerContext}: one bean per context, destroyed when the context closes. */
@Singleton
public final class PerContextScope extends AbstractConcurrentCustomScope<PerContext> {

    private final Map<BeanIdentifier, CreatedBean<?>> beans = new HashMap<>();

    public PerContextScope() {
        super(PerContext.class);
    }

    @Override
    protected Map<BeanIdentifier, CreatedBean<?>> getScopeMap(boolean forCreation) {
        return beans;
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public void close() {
        destroyScope(beans);
    }
}
