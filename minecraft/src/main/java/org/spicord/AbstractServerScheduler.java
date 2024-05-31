package org.spicord;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

    private boolean isShutdown = false;

    @Override
    public void execute(Runnable command) {
        if (isShutdown) {
            return;
        }
        runTaskAsync(command);
    }

    public static interface ServerTask {

        boolean isCancelled();

        void cancel();
    }

    protected abstract ServerTask runTaskAsync(Runnable runnable);

    protected abstract ServerTask runTaskAsyncLater(Runnable runnable, TimeUnit unit, long delay);

    protected abstract ServerTask runTaskAsyncLaterRepeating(Runnable runnable, TimeUnit unit, long delay, long period);

    @Override
    public boolean isShutdown() {
        return isShutdown;
    }

    @Override
    public boolean isTerminated() {
        return isShutdown; // TODO
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return new BukkitFuture<>(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return new BukkitFuture<>(toCallable(task, result));
    }

    @Override
    public Future<?> submit(Runnable task) {
        return new BukkitFuture<>(toCallable(task));
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
        return new BukkitFuture<>(toCallable(command), unit, delay);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return new BukkitFuture<>(callable, unit, delay);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return new BukkitFuture<>(toCallable(command), unit, initialDelay, period);
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
        isShutdown = true;
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdown();
        return Collections.emptyList(); // TODO: Return list of non-executed tasks
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return true; // TODO
    }

    private class BukkitFuture<T> implements ScheduledFuture<T> {

        private final ServerTask task;
        private final AtomicReference<T> ret;
        private final AtomicReference<Throwable> ex;
        private final AtomicBoolean done;

        public BukkitFuture(Callable<T> callable) {
            this.ret = new AtomicReference<>();
            this.ex = new AtomicReference<>();
            this.done = new AtomicBoolean(false);

            final Runnable r = () -> {
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

            this.task = runTaskAsync(r);
        }

        public BukkitFuture(Callable<T> callable, TimeUnit unit, long delay) {
            this.ret = new AtomicReference<>();
            this.ex = new AtomicReference<>();
            this.done = new AtomicBoolean(false);

            final Runnable r = () -> {
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

            this.task = runTaskAsyncLater(r, unit, delay);
        }

        public BukkitFuture(Callable<T> callable, TimeUnit unit, long delay, long period) {
            this.ret = new AtomicReference<>();
            this.ex = new AtomicReference<>();
            this.done = new AtomicBoolean(false);

            final Runnable r = () -> {
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

            this.task = runTaskAsyncLaterRepeating(r, unit, delay, period);
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
