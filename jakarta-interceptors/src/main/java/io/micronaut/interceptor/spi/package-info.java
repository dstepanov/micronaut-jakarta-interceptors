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
 * The interfaces and helpers another module building on the interception uses, as micronaut-cdi does: which
 * interceptor classes are enabled ({@link io.micronaut.interceptor.spi.BoundInterceptorEnablement}), and the
 * interceptor methods of an interceptor class ({@link io.micronaut.interceptor.spi.InterceptorMethods}).
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@NullMarked
package io.micronaut.interceptor.spi;

import org.jspecify.annotations.NullMarked;
