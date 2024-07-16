package org.spicord.velocity.server;

import java.util.concurrent.TimeUnit;

import org.spicord.AbstractServerScheduler;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler.TaskBuilder;
import com.velocitypowered.api.scheduler.TaskStatus;

public class VelocityServerScheduler extends AbstractServerScheduler {

    private ProxyServer server;
    private Object plugin;

    public VelocityServerScheduler(ProxyServer server, Object plugin) {
        this.server = server;
        this.plugin = plugin;
    }

    @Override
    protected boolean isPluginEnabled() {
        return server.getPluginManager().isLoaded("spicord");
    }

    private TaskBuilder buildTask(Runnable r) {
        return server.getScheduler().buildTask(plugin, r);
    }

    @Override
    protected ServerTask runTaskAsync(Runnable runnable) {
        return new ServerTaskImpl(buildTask(runnable).schedule());
    }

    @Override
    protected ServerTask runTaskAsyncLater(Runnable runnable, TimeUnit unit, long delay) {
        return new ServerTaskImpl(buildTask(runnable).delay(delay, unit).schedule());
    }

    @Override
    protected ServerTask runTaskAsyncLaterRepeating(Runnable runnable, TimeUnit unit, long delay, long period) {
        return new ServerTaskImpl(buildTask(runnable).delay(delay, unit).repeat(period, unit).schedule());
    }

    private class ServerTaskImpl implements ServerTask {
        private final ScheduledTask task;

        public ServerTaskImpl(ScheduledTask task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            task.cancel();
        }

        @Override
        public boolean isCancelled() {
            return task.status() == TaskStatus.CANCELLED;
        }
    }
}
