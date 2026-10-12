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

import io.micronaut.aop.MethodInvocationContext;

/**
 * The {@link MicronautInvocationContext} of the interception of a method: a business method, a timeout method, or
 * a lifecycle callback of a bean.
 *
 * @author Denis Stepanov
 * @since 1.0
 * @see MicronautConstructorInvocationContext
 */
public interface MicronautMethodInvocationContext extends MicronautInvocationContext {

    /**
     * The Micronaut invocation this interception is part of.
     *
     * <p>Its {@link MethodInvocationContext#getExecutableMethod() executable method} is the one Micronaut generated
     * for the intercepted method while the application was compiled. It describes what {@link #getMethod()}
     * describes - the declaring type, the name, the arguments and the annotation metadata of the method - and
     * reading any of that reflects on nothing. Invoking it dispatches through generated code rather than through
     * {@link java.lang.reflect.Method#invoke}, which is how the interception invokes the intercepted method
     * itself.</p>
     *
     * <pre>{@code
     * MethodInvocationContext<Object, Object> invocation = micronaut.getMicronautInvocation();
     * ExecutableMethod<Object, Object> method = invocation.getExecutableMethod();
     * method.getDeclaringType();      // the class that declared it
     * method.getMethodName();         // its name
     * method.getArguments();          // its arguments, with their generic types
     * Object id = invocation.getParameters().get("id").getValue();   // an argument by name
     * }</pre>
     *
     * <p>The whole invocation is here for what the specification does not reach as well: the arguments as a named
     * map through {@link MethodInvocationContext#getParameters()}, the attributes shared with every other advice on
     * the same invocation, and whatever a later version of Micronaut adds. {@link #getAnnotationMetadata()} is the
     * metadata it carries.</p>
     *
     * <p>A lifecycle interception runs one chain for the whole event rather than one for each callback, so there
     * is no single callback it is the interception of. The chain describes itself by the last callback it invokes,
     * which is the most derived - the one the intercepted class declares, where it declares one - and that is the
     * executable method of the invocation. It agrees with {@link #getMethod()}, which the specification defines
     * the same way. A bean bound for a kind it declares no callback of has neither: the executable method is the
     * event itself, named {@code initialize} or {@code dispose}, whose {@code hasTargetMethod()} is
     * {@code false}.</p>
     *
     * <p>Do not call {@link MethodInvocationContext#proceed()} on it. That is what the last interceptor method of
     * the chain proceeds into, so proceeding it directly runs the intercepted method while skipping every
     * interceptor method the specification has yet to invoke. Call {@link #proceed()} on this context instead,
     * which is the chain the specification describes.</p>
     *
     * <p>The same caution applies to {@code setParameterValue}: the arguments are replaced
     * through {@link #setParameters(Object[])}, which checks their count and their types and shows an interceptor
     * only the arguments the intercepted method declares.</p>
     *
     * @return The invocation, never {@code null}
     */
    @Override
    MethodInvocationContext<Object, Object> getMicronautInvocation();
}
