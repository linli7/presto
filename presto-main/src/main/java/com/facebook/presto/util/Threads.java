/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.util;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.util.concurrent.MoreExecutors.newDirectExecutorService;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public final class Threads
{
    private static final Class<? extends ExecutorService> GUAVA_DIRECT_THREAD_EXECUTOR_CLASS = newDirectExecutorService().getClass();

    private Threads() {}

    public static Executor checkNotSameThreadExecutor(Executor executor, String name)
    {
        if (executor == null) {
            throw new NullPointerException(format("%s is null", name));
        }
        checkArgument(!isSameThreadExecutor(executor), "%s is a same thread executor", name);
        return executor;
    }

    public static boolean isSameThreadExecutor(Executor executor)
    {
        requireNonNull(executor, "executor is null");
        if (executor.getClass() == GUAVA_DIRECT_THREAD_EXECUTOR_CLASS) {
            return true;
        }

        Thread thisThread = Thread.currentThread();
        SettableFuture<Boolean> isSameThreadExecutor = SettableFuture.create();
        executor.execute(() -> isSameThreadExecutor.set(thisThread == Thread.currentThread()));
        try {
            return Futures.getChecked(isSameThreadExecutor, Exception.class, 10, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw Throwables.propagate(e);
        }
        catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
