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

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;

/**
 * What {@code InvocationContext.getTimer()} returns for a method the scheduler invokes.
 *
 * <p>The specification returns the timer of the Jakarta Enterprise Beans timer service, of which there is none
 * here. What there is instead is the schedule the method was registered with, which is the same thing an
 * interceptor would want it for, and which is read from the annotation that declared it.</p>
 *
 * @param cron         The cron expression the method is scheduled with, empty when it is not
 * @param zoneId       The time zone of the cron expression, empty when it is the default one
 * @param fixedDelay   The delay between the end of one invocation and the start of the next, empty when unset
 * @param fixedRate    The interval between the starts of two invocations, empty when unset
 * @param initialDelay The delay before the first invocation, empty when unset
 * @param scheduler    The name of the scheduler that invokes the method
 * @author Denis Stepanov
 * @since 1.0
 */
public record ScheduledTimer(String cron,
                             String zoneId,
                             String fixedDelay,
                             String fixedRate,
                             String initialDelay,
                             String scheduler) {

    /**
     * Reads the schedule of a method from the annotation that declared it.
     *
     * @param scheduled The {@code io.micronaut.scheduling.annotation.Scheduled} annotation
     * @return The timer
     */
    @Internal
    static ScheduledTimer of(AnnotationValue<?> scheduled) {
        return new ScheduledTimer(
            scheduled.stringValue("cron").orElse(""),
            scheduled.stringValue("zoneId").orElse(""),
            scheduled.stringValue("fixedDelay").orElse(""),
            scheduled.stringValue("fixedRate").orElse(""),
            scheduled.stringValue("initialDelay").orElse(""),
            scheduled.stringValue("scheduler").orElse(""));
    }
}
