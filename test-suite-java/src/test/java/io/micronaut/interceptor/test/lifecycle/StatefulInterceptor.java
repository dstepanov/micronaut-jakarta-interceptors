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
package io.micronaut.interceptor.test.lifecycle;

import jakarta.annotation.PostConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An interceptor class that keeps state of its own: what it counted for the object it interposes on, and
 * nothing of any other object's.
 */
@Interceptor
@Stateful
public class StatefulInterceptor {

    /**
     * What each instance of this interceptor counted by the time a method returned, in the order the
     * interceptions happened.
     */
    public static final List<Integer> COUNTS = new CopyOnWriteArrayList<>();

    /**
     * How many instances of this interceptor were constructed.
     */
    public static final AtomicInteger CONSTRUCTED = new AtomicInteger();

    private final AtomicInteger calls = new AtomicInteger();

    @PostConstruct
    void created(InvocationContext context) throws Exception {
        CONSTRUCTED.incrementAndGet();
        context.proceed();
    }

    @AroundInvoke
    Object counted(InvocationContext context) throws Exception {
        Object result = context.proceed();
        COUNTS.add(calls.incrementAndGet());
        return result;
    }
}
