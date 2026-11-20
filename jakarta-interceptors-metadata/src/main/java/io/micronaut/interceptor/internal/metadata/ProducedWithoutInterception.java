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
package io.micronaut.interceptor.internal.metadata;

import io.micronaut.core.annotation.Internal;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a member of an intercepted factory whose product nothing binds: the construction and the lifecycle callbacks
 * of the bean it produces are not intercepted.
 *
 * <p>Micronaut reads the metadata of a bean a factory produces, and of its construction, together with the metadata
 * of the factory class, so the {@link JakartaInterception} a factory declares for itself would otherwise reach the
 * produced object, which is no instance of the factory (CDI 4.1 sections 7.2 and 9). The member cannot carry an
 * empty {@link JakartaInterception} in its place: that is an around binding, and would have Micronaut advise the
 * product, which nothing binds and which may not be proxyable. This annotation binds nothing; the runtime reads it
 * where it builds the chains of an object, and not where it builds the ones of a business method, so the producer
 * method stays a business method of the factory.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Internal
public @interface ProducedWithoutInterception {
}
