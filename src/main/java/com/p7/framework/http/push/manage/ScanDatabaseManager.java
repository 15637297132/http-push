package com.p7.framework.http.push.manage;

import com.p7.framework.http.push.task.ScanDatabaseTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Yangzhen
 **/
public class ScanDatabaseManager implements DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScanDatabaseManager.class);

    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    public void addTask(ScanDatabaseTask task) {
        ScheduledFuture<?> sdf = scheduledThreadPoolExecutor.scheduleWithFixedDelay(task, task.getFirstDelayedMs(), task.getDelayedCycleMs(), TimeUnit.MILLISECONDS);
        task.setScheduledFuture(sdf);
        LOGGER.info("add scan database task , appId is {} , appType is {} , firstDelayedMs is {} , delayedCycleMs is {}", task.getAppId(), task.getPushType(), task.getFirstDelayedMs(), task.getDelayedCycleMs());
    }

    /**
     * scheduledThreadPoolExecutor中的延时队列存放的任务是scheduledThreadPoolExecutor的内部类RunnableScheduledFuture，因此要强转
     * @param task
     * @param sdf
     */
    public void removeTask(ScanDatabaseTask task, ScheduledFuture<?> sdf) {
        LOGGER.info("remove scan database task , appId is {} , appType is {}", task.getAppId(), task.getPushType());
        scheduledThreadPoolExecutor.remove((RunnableScheduledFuture) sdf);
    }

    /**
     * Scheduled定时任务的数量也会包含在内
     *
     * @return
     */
    public int size() {
        return scheduledThreadPoolExecutor.getQueue().size();
    }

    @Override
    public void destroy() throws Exception {
        if (scheduledThreadPoolExecutor != null) {
            scheduledThreadPoolExecutor.shutdown();
        }
    }

    public void setScheduledThreadPoolExecutor(ScheduledThreadPoolExecutor scheduledThreadPoolExecutor) {
        this.scheduledThreadPoolExecutor = scheduledThreadPoolExecutor;
    }

}
