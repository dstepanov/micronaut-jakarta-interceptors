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

import io.micronaut.core.annotation.Internal;

import java.util.List;

/**
 * What a bean definition keeps of the interception of the objects it creates, through
 * {@link io.micronaut.inject.BeanDefinition#getCachedUnevaluatedValue}.
 *
 * <p>The chains of a construction and of the lifecycle events depend on the definition and on the interceptor classes
 * of its context, and so belong on the definition, which Micronaut keeps per context and hands to the advice: as the
 * bean it is created for, and through the context of a construction or of a lifecycle event. A definition wrapping
 * another, as the ones of an {@code @EachBean} do, keeps its values on the one it wraps. They are built from the
 * interception the processor recorded, which holds no evaluated expression.</p>
 *
 * @param construction       The chain of the construction of an object, read from the constructor
 * @param postConstruct      The chain of its post-construct event, read from the class
 * @param preDestroy         The chain of its pre-destroy event, read from the class
 * @param interceptorClasses The interceptor classes whose instances are created with an object
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
record ObjectChains(List<InterceptorReference> construction,
                    List<InterceptorReference> postConstruct,
                    List<InterceptorReference> preDestroy,
                    List<Class<?>> interceptorClasses) {
}
