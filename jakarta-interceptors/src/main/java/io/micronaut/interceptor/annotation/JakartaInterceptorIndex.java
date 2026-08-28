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
package io.micronaut.interceptor.annotation;

import io.micronaut.core.annotation.Internal;
import jakarta.interceptor.InvocationContext;

/**
 * The type the interceptor classes of an application are found through.
 *
 * <p>The processor adapts one interceptor method of every interceptor class to this type, which gives each of them
 * a bean definition of a type the context indexes. The runtime asks the context for the definitions of this type
 * to find the interceptor classes, instead of reading every bean definition there is and keeping the ones that are
 * interceptor classes; the definition it gets back carries the metadata of the interceptor class it was adapted
 * from, which is what the chains are resolved by.</p>
 *
 * <p>Nothing invokes it. Adapting a method is what produces the definition; the interceptor methods themselves are
 * invoked through the executable methods the processor declared for them, on the instance the advice holds.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@FunctionalInterface
@Internal
public interface JakartaInterceptorIndex {

    /**
     * Never invoked; the signature is the one of an interceptor method that returns the result of what it
     * interposes on.
     *
     * @param context The context of an invocation
     * @return The result
     * @throws Exception Whatever the interceptor method throws
     */
    Object invoke(InvocationContext context) throws Exception;
}
