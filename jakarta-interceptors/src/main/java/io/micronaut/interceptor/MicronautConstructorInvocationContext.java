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
package io.micronaut.interceptor;

import io.micronaut.core.beans.BeanConstructor;

/**
 * The {@link MicronautInvocationContext} of an {@code @AroundConstruct} interception.
 *
 * @author Denis Stepanov
 * @since 1.0
 * @see MicronautMethodInvocationContext
 */
public interface MicronautConstructorInvocationContext extends MicronautInvocationContext {

    /**
     * The constructor Micronaut instantiates the bean with, as it compiled it.
     *
     * <p>It describes what {@link #getConstructor()} describes - the declaring type, the arguments and the
     * annotation metadata of the constructor - and reading any of that reflects on nothing.</p>
     *
     * <pre>{@code
     * BeanConstructor<?> constructor = micronaut.getBeanConstructor();
     * constructor.getDeclaringBeanType();   // the bean being constructed
     * constructor.getArguments();           // the arguments it declares
     * }</pre>
     *
     * <p>Where the bean is one Micronaut also generated a proxy of, because it has around advice as well as
     * constructor interception, this is the constructor of the target class rather than the one of the proxy: the
     * declaring type is the bean, and the arguments are the ones the bean declares rather than those followed by
     * the handful the proxy adds. That is the constructor the specification shows an interceptor, and it is what
     * {@link #getParameters()} lines up with.</p>
     *
     * <p>{@link #getTarget()} is {@code null} until the chain has proceeded, since the instance does not exist
     * before then. The constructor is known throughout.</p>
     *
     * @return The constructor, never {@code null}
     */
    BeanConstructor<Object> getBeanConstructor();
}
