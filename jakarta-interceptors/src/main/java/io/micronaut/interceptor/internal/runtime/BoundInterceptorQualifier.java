/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.interceptor.internal.runtime;

import io.micronaut.context.Qualifier;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.BeanType;
import io.micronaut.interceptor.spi.InterceptorBindings;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

/**
 * Selects the interceptor classes bound to an interception point: the ones every binding of which is a binding of
 * the point.
 *
 * <p>Micronaut's own {@code InterceptorBindingQualifier} selects an interceptor sharing any one binding with the
 * point, where the specification requires all of the interceptor's, so the comparison is
 * {@link InterceptorBindings#isBoundBy(InterceptorBindings)}. It is a filter, shaped as Micronaut's
 * {@code FilteringQualifier} is, which is internal to Micronaut and so is not extended.</p>
 *
 * @param <T> The type the beans are looked up by
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class BoundInterceptorQualifier<T> implements Qualifier<T> {

    private final InterceptorBindings point;

    /**
     * @param point The bindings of the interception point
     */
    public BoundInterceptorQualifier(InterceptorBindings point) {
        this.point = point;
    }

    @Override
    public <BT extends BeanType<T>> Stream<BT> reduce(Class<T> beanType, Stream<BT> candidates) {
        return candidates.filter(candidate -> doesQualify(beanType, candidate));
    }

    @Override
    public boolean doesQualify(Class<T> beanType, BeanType<T> candidate) {
        return InterceptorBindings.of(candidate).isBoundBy(point);
    }

    @Override
    public <BT extends BeanType<T>> Collection<BT> filter(Class<T> beanType, Collection<BT> candidates) {
        Collection<BT> bound = new ArrayList<>(candidates.size());
        for (BT candidate : candidates) {
            if (doesQualify(beanType, candidate)) {
                bound.add(candidate);
            }
        }
        return bound;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return this == o || (o instanceof BoundInterceptorQualifier<?> other && point.equals(other.point));
    }

    @Override
    public int hashCode() {
        return point.hashCode();
    }

    @Override
    public String toString() {
        return "BoundTo" + point;
    }
}
