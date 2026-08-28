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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Records, on an interceptor class, which of its methods interposes on what.
 *
 * <p>The interceptor methods are named rather than looked up again at runtime: the processor has already found and
 * validated them, and the names are what the advice resolves the executable methods Micronaut generated for them
 * by, so that an interceptor method is invoked without reflection.</p>
 *
 * <p>Each kind names as many methods as the class and its superclasses declare of it, in the order the
 * specification invokes them: the most general superclass first.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Internal
public @interface JakartaInterceptorMethods {

    /**
     * The {@code jakarta.interceptor.AroundInvoke} methods the class and its superclasses declare.
     *
     * @return The method names, most general superclass first
     */
    String[] aroundInvoke() default {};

    /**
     * The {@code jakarta.interceptor.AroundTimeout} methods the class and its superclasses declare.
     *
     * @return The method names, most general superclass first
     */
    String[] aroundTimeout() default {};

    /**
     * The {@code jakarta.interceptor.AroundConstruct} methods the class and its superclasses declare.
     *
     * @return The method names, most general superclass first
     */
    String[] aroundConstruct() default {};

    /**
     * The {@code jakarta.annotation.PostConstruct} methods the class and its superclasses declare.
     *
     * @return The method names, most general superclass first
     */
    String[] postConstruct() default {};

    /**
     * The {@code jakarta.annotation.PreDestroy} methods the class and its superclasses declare.
     *
     * @return The method names, most general superclass first
     */
    String[] preDestroy() default {};

    /**
     * The binding annotations the interceptor class declares, each written out as one string by the processor,
     * the same way the ones of an intercepted element are.
     *
     * @return The bindings of the interceptor class
     */
    String[] bindings() default {};
}
