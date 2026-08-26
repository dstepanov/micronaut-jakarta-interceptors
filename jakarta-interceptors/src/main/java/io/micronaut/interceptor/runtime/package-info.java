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
/**
 * The runtime of the Jakarta Interceptors implementation: the advice that runs the interceptor chains and the
 * {@code InvocationContext} implementations it hands to the interceptor methods.
 *
 * <p>Nothing here is reflective. An interceptor method is invoked through the executable method Micronaut
 * generated for it at compilation time, the chain that applies to an element is read from the annotations the
 * processor left on it, and the interceptor instances come from the bean context. Three accessors of
 * {@code InvocationContext} do reach for the reflection of the platform, because the types the specification has
 * them return leave no choice: {@code getMethod()} and {@code getConstructor()} return a
 * {@code java.lang.reflect} member, and {@code getInterceptorBindings()} returns annotation instances. All three
 * are resolved only when an interceptor asks for them, and remembered afterwards, so an application whose
 * interceptors do not use them never reflects at all.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@NullMarked
package io.micronaut.interceptor.runtime;

import org.jspecify.annotations.NullMarked;
