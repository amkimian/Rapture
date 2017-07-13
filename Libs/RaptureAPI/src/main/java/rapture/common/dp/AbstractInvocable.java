/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2011-2016 Incapture Technologies LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package rapture.common.dp;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rapture.common.CallingContext;

public abstract class AbstractInvocable<T> implements Steps {
    public static final String ABORT = "__reserved__ABORTED";
    public static final String TIMEOUT = "__reserved__TIMEOUT";
    private String workerUri;
    private Long stepStartTime;
    private ClassLoader classLoader;

    private String stepName;
    private String errName;

    public AbstractInvocable(String workerUri, String stepName) {
        // TODO Disallow null or empty string, but that breaks several test cases
        this.workerUri = workerUri;
        this.stepName = stepName;
        errName = stepName + "Error";
    }

    public void preInvoke(CallingContext ctx) {
        // Intended to be overrideable to allow any setup that requires CallingContext
        // and therefore cannot be done in the constructor.
        // Could make this abstract, but there are about 20 implementing classes.
    }

    public Long getStepStartTime() {
        return stepStartTime;
    }

    public void setStepStartTime(Long stepStartTime) {
        this.stepStartTime = stepStartTime;
    }

    public String getStepName() {
        return stepName;
    }

    public String getErrName() {
        return errName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
        this.errName = stepName + "Error";
    }

    public String getWorkerURI() {
        return workerUri;
    }

    public abstract String invoke(CallingContext ctx);

    /**
     * Override for any invocable that need to do any cleanup before its thread it killed
     * 
     * @return "InvocableUtils.ABORTED or a regular return value if the job finished before it could be cancelled.
     */
    public String abortableInvoke(final CallingContext ctx, int timeoutSeconds) {
        final T handle = prepareInterruptableInvocation(ctx);

        FutureTask<String> task = new FutureTask<>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return invokeHook(ctx, handle);
            }
        });
        ExecutorService executor = Executors.newFixedThreadPool(1);
        executor.execute(task);
        try {
            return task.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            task.cancel(true);
            doInterrupt(handle);
            return ABORT;
        } catch (ExecutionException e) {
            return ERROR;
        } catch (TimeoutException e) {
            task.cancel(true);
            doInterrupt(handle);
            executor.shutdownNow();
            return TIMEOUT;
        }
    }

    public void doInterrupt(T obj) {
        // do nothing by default
    }

    public T prepareInterruptableInvocation(CallingContext ctx) {
        return null;
    }

    public String invokeHook(CallingContext ctx, T handle) {
        return invoke(ctx);
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    // Can be overridden
    public static String getNextTransition() {
        return Steps.NEXT;
    }

    public static String getErrorTransition() {
        return Steps.ERROR;
    }

    public static String getFailTransition() {
        return Steps.QUIT;
    }

}
