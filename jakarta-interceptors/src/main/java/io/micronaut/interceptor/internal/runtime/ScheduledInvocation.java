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

import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.propagation.PropagatedContext;
import io.micronaut.core.propagation.PropagatedContextElement;
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
     * Enters the invocation of a method by the scheduler, when the scheduler is the one invoking it.
     *
     * <p>The scheduler puts its invocation in the propagated context of the call, so a method the scheduled method
     * calls, or work it propagates to another thread, sees it as well; only the method the scheduler invoked is
     * answered. Once entered, the execution is marked as entered in the propagated context for as long as the
     * returned scope is open, so that the method calling itself, which matches the execution as well, is invoked as a
     * business method. A later execution by the scheduler is another one, and is entered again.</p>
     *
     * @param context The invocation
     * @return The entered invocation, to be closed once the invocation returns, or {@code null} when the scheduler
     * did not invoke the method
     */
    static @Nullable Entry enter(MethodInvocationContext<?, ?> context) {
        ScheduledExecution execution = ScheduledExecution.current().orElse(null);
        if (execution == null || !invokes(execution, context)) {
            return null;
        }
        PropagatedContext propagated = PropagatedContext.getOrEmpty();
        Entered entered = propagated.find(Entered.class).orElse(null);
        if (entered != null && entered.execution() == execution) {
            return null;
        }
        return new Entry(execution.schedule(), propagated.plus(new Entered(execution)).propagate());
    }

    private static boolean invokes(ScheduledExecution execution, MethodInvocationContext<?, ?> context) {
        ExecutableMethod<?, ?> scheduled = execution.method();
        return scheduled.getMethodName().equals(context.getMethodName())
            && Arrays.equals(scheduled.getArgumentTypes(), context.getArgumentTypes())
            && (scheduled.getDeclaringType().isAssignableFrom(context.getDeclaringType())
                || context.getDeclaringType().isAssignableFrom(scheduled.getDeclaringType()));
    }

    /**
     * An invocation by the scheduler that has been entered.
     *
     * @param schedule The {@code @Scheduled} annotation that triggered it
     * @param scope    The scope that marks the execution as entered
     */
    record Entry(AnnotationValue<?> schedule, PropagatedContext.Scope scope) implements AutoCloseable {
        @Override
        public void close() {
            scope.close();
        }
    }

    /**
     * Marks an execution of the scheduler as already entered, compared by identity.
     *
     * @param execution The execution
     */
    private record Entered(ScheduledExecution execution) implements PropagatedContextElement {
    }
}
