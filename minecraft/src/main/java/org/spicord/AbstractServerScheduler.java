package org.spicord;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractServerScheduler implements ScheduledExecutorService {

    @Override
    public void execute(Runnable command) {
        runTaskAsync(command);
    }

    public static interface ServerTask {

        boolean isCancelled();

        void cancel();

    }

    protected abstract boolean isPluginEnabled();

    protected abstract ServerTask runTaskAsync(Runnable runnable);

    protected abstract ServerTask runTaskAsyncLater(Runnable runnable, TimeUnit unit, long delay);

    protected abstract ServerTask runTaskAsyncLaterRepeating(Runnable runnable, TimeUnit unit, long delay, long period);

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return new ScheduledFutureImpl<>(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return new ScheduledFutureImpl<>(toCallable(task, result));
    }

    @Override
    public Future<?> submit(Runnable task) {
        return new ScheduledFutureImpl<>(toCallable(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        List<Future<T>> list = new ArrayList<>();
        for (Callable<T> task : tasks) {
            Future<T> future = submit(task);
            list.add(future);
        }
        return list;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        Exception lastEx = null;
        for (Callable<T> task : tasks) {
            try {
                return task.call(); // TODO: Run tasks in parallel
            } catch (Exception e) {
                lastEx = e;
            }
        }
        throw new ExecutionException(lastEx);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return new ScheduledFutureImpl<>(toCallable(command), unit, delay);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return new ScheduledFutureImpl<>(callable, unit, delay);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return new ScheduledFutureImpl<>(toCallable(command), unit, initialDelay, period);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return scheduleAtFixedRate(command, initialDelay, delay, unit);
    }

    private static <V> Callable<V> toCallable(Runnable command) {
        return toCallable(command, null);
    }

    private static <V> Callable<V> toCallable(Runnable command, V result) {
        return () -> { command.run(); return result; };
    }

    // ============================================================

    @Override
    public void shutdown() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public List<Runnable> shutdownNow() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException("not implemented");
    }

    private class ScheduledFutureImpl<T> implements ScheduledFuture<T> {

        private final ServerTask task;
        private final AtomicReference<T> ret = new AtomicReference<>();
        private final AtomicReference<Throwable> ex = new AtomicReference<>();
        private final AtomicBoolean done = new AtomicBoolean(false);

        private Runnable createRunnable(Callable<T> callable) {
            return () -> {
                synchronized (done) {
                    try {
                        ret.set(callable.call());
                    } catch (Throwable e) {
                        ex.set(e);
                    }
                    done.set(true);
                    done.notify();
                }
            };
        }

        public ScheduledFutureImpl(Callable<T> callable) {
            this.task = runTaskAsync(createRunnable(callable));
        }

        public ScheduledFutureImpl(Callable<T> callable, TimeUnit unit, long delay) {
            this.task = runTaskAsyncLater(createRunnable(callable), unit, delay);
        }

        public ScheduledFutureImpl(Callable<T> callable, TimeUnit unit, long delay, long period) {
            this.task = runTaskAsyncLaterRepeating(createRunnable(callable), unit, delay, period);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            task.cancel();
            return true;
        }

        @Override
        public boolean isCancelled() {
            return task.isCancelled();
        }

        @Override
        public boolean isDone() {
            return done.get();
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            try {
                return get(0, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            synchronized (done) {
                if (!done.get()) { // if not done
                    done.wait(unit.toMillis(timeout)); // wait
                }
            }

            if (ex.get() != null) {
                throw new ExecutionException(ex.get());
            }
            return ret.get();
        }

        @Override
        public long getDelay(TimeUnit unit) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public int compareTo(Delayed o) {
            throw new UnsupportedOperationException("not implemented");
        }
    }
}
