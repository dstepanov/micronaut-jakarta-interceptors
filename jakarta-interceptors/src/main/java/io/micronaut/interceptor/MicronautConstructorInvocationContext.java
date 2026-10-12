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

import io.micronaut.aop.ConstructorInvocationContext;

/**
 * The {@link MicronautInvocationContext} of an {@code @AroundConstruct} interception.
 *
 * @author Denis Stepanov
 * @since 1.0
 * @see MicronautMethodInvocationContext
 */
public interface MicronautConstructorInvocationContext extends MicronautInvocationContext {

    /**
     * The Micronaut construction this interception is part of.
     *
     * <p>Its {@link ConstructorInvocationContext#getConstructor() constructor} is the one Micronaut instantiates
     * the bean with, as it compiled it. It describes what {@link #getConstructor()} describes - the declaring type,
     * the arguments and the annotation metadata of the constructor - and reading any of that reflects on
     * nothing.</p>
     *
     * <pre>{@code
     * BeanConstructor<Object> constructor = micronaut.getMicronautInvocation().getConstructor();
     * constructor.getDeclaringBeanType();   // the bean being constructed
     * constructor.getArguments();           // the arguments it declares
     * }</pre>
     *
     * <p>Where the bean is one Micronaut also generated a proxy of, because it has around advice as well as
     * constructor interception, that is the constructor of the target class rather than the one of the proxy: the
     * declaring type is the bean, and the arguments are the ones the bean declares rather than those followed by
     * the handful the proxy adds. That is the constructor the specification shows an interceptor, and it is what
     * {@link #getParameters()} lines up with. {@link #getAnnotationMetadata()} is its metadata.</p>
     *
     * <p>{@link #getTarget()} is {@code null} until the chain has proceeded, since the instance does not exist
     * before then. The constructor is known throughout.</p>
     *
     * <p>Do not call {@link ConstructorInvocationContext#proceed()} on it: that is what the last interceptor
     * method of the chain proceeds into, and proceeding it directly constructs the bean while skipping every
     * interceptor method the specification has yet to invoke, leaving this context without the instance that
     * {@link #getTarget()} is meant to answer with. Call {@link #proceed()} on this context instead.</p>
     *
     * @return The construction, never {@code null}
     */
    @Override
    ConstructorInvocationContext<Object> getMicronautInvocation();
}
