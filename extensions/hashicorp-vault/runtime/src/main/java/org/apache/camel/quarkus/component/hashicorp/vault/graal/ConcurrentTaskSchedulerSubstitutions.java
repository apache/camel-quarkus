/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.quarkus.component.hashicorp.vault.graal;

import java.time.Clock;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.TaskUtils;
import org.springframework.util.ErrorHandler;

/**
 * Remove the JSR-236 (jakarta.enterprise.concurrent) code paths of Spring's ConcurrentTaskScheduler, used by
 * camel-hashicorp-vault for token renewal. The JSR-236 API is not on the classpath.
 */
@TargetClass(className = "org.springframework.scheduling.concurrent.ConcurrentTaskScheduler")
final class ConcurrentTaskSchedulerSubstitutions {
    @Alias
    private ErrorHandler errorHandler;

    @Alias
    private Clock clock;

    @Alias
    private ScheduledExecutorService getScheduledExecutor() {
        return null;
    }

    @Substitute
    public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
        ScheduledExecutorService scheduleExecutorToUse = getScheduledExecutor();
        try {
            ErrorHandler errorHandlerToUse = (this.errorHandler != null ? this.errorHandler
                    : TaskUtils.getDefaultErrorHandler(true));
            return new ReschedulingRunnableAlias(
                    ((ConcurrentTaskExecutorAlias) (Object) this).decorateTaskIfNecessary(task), trigger, this.clock,
                    scheduleExecutorToUse, errorHandlerToUse)
                    .schedule();
        } catch (RejectedExecutionException ex) {
            throw new TaskRejectedException(scheduleExecutorToUse, task, ex);
        }
    }
}

@TargetClass(className = "org.springframework.scheduling.concurrent.ConcurrentTaskExecutor")
final class ConcurrentTaskExecutorAlias {
    @Alias
    Runnable decorateTaskIfNecessary(Runnable task) {
        return null;
    }
}

@TargetClass(className = "org.springframework.scheduling.concurrent.ReschedulingRunnable")
final class ReschedulingRunnableAlias {
    @Alias
    public ReschedulingRunnableAlias(Runnable delegate, Trigger trigger, Clock clock,
            ScheduledExecutorService executor, ErrorHandler errorHandler) {
    }

    @Alias
    public ScheduledFuture<?> schedule() {
        return null;
    }
}

@TargetClass(className = "org.springframework.scheduling.concurrent.ConcurrentTaskExecutor$ManagedTaskBuilder")
final class ManagedTaskBuilderSubstitutions {
    @Substitute
    public static Runnable buildManagedTask(Runnable task, String identityName) {
        return task;
    }

    @Substitute
    public static <T> Callable<T> buildManagedTask(Callable<T> task, String identityName) {
        return task;
    }
}
