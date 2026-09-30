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
package io.micronaut.interceptor.runtime;

import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.scheduling.ScheduledExecution;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * Tells whether the scheduler is invoking a method, and by which of its schedules.
 *
 * <p>Only reached for a method the processor recorded as scheduled, which a method is only when it declares
 * {@code @Scheduled}: micronaut-context, which declares the annotation and invokes the method, is then present.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
final class ScheduledInvocation {

    private ScheduledInvocation() {
    }

    /**
     * The schedule that triggered the invocation of a method.
     *
     * <p>The scheduler records its invocation for the whole of the call, so a method the scheduled method calls
     * sees it as well; only the method the scheduler invoked is answered.</p>
     *
     * @param context The invocation
     * @return The {@code @Scheduled} annotation that triggered it, or {@code null} when the scheduler did not
     */
    static @Nullable AnnotationValue<?> scheduleOf(MethodInvocationContext<?, ?> context) {
        ScheduledExecution execution = ScheduledExecution.current().orElse(null);
        if (execution == null) {
            return null;
        }
        ExecutableMethod<?, ?> scheduled = execution.method();
        if (scheduled.getMethodName().equals(context.getMethodName())
            && Arrays.equals(scheduled.getArgumentTypes(), context.getArgumentTypes())
            && (scheduled.getDeclaringType().isAssignableFrom(context.getDeclaringType())
                || context.getDeclaringType().isAssignableFrom(scheduled.getDeclaringType()))) {
            return execution.schedule();
        }
        return null;
    }
}
