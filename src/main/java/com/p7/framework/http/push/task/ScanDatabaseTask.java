package com.p7.framework.http.push.task;

import com.p7.framework.http.push.manage.ScanDatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledFuture;

/**
 * 扫描数据库任务
 *
 * @author Yangzhen
 **/
public class ScanDatabaseTask implements Runnable {

    private Logger logger = LoggerFactory.getLogger(ScanDatabaseTask.class);

    private BatchPushTask batchPushTask;

    private ScanDatabaseManager scanDatabaseManager;

    private ScheduledFuture<?> scheduledFuture;

    public ScanDatabaseTask(BatchPushTask batchPushTask, ScanDatabaseManager scanDatabaseManager) {
        this.batchPushTask = batchPushTask;
        this.scanDatabaseManager = scanDatabaseManager;
    }

    @Override
    public void run() {
        logger.info("##### appId is {} , pushType is {} , scan database started...", batchPushTask.getAppId(), batchPushTask.getPushType());

        try {
            if (batchPushTask.destroyed) {
                scanDatabaseManager.removeTask(this, scheduledFuture);
            } else {
                if (batchPushTask.getChangeScanStrategy()) {
                    scanDatabaseManager.removeTask(this, scheduledFuture);
                    scanDatabaseManager.addTask(this);
                    batchPushTask.setChangeScanStrategy(false);
                    logger.info("scan database task reset , appId is {} , pushType is {}", batchPushTask.getAppId(), batchPushTask.getPushType());
                }
            }
            batchPushTask.triggerScan();
        } catch (Exception e) {
            logger.error("appId is {} , pushType is {} , catch exception {}", batchPushTask.getAppId(), batchPushTask.getPushType(), e.getMessage());
        }
    }

    public int getFirstDelayedMs() {
        return batchPushTask.getFirstDelayedMs();
    }

    public int getDelayedCycleMs() {
        return batchPushTask.getDelayedCycleMs();
    }

    public int getAppId() {
        return batchPushTask.getAppId();
    }

    public int getPushType() {
        return batchPushTask.getPushType();
    }

    public ScheduledFuture<?> getScheduledFuture() {
        return scheduledFuture;
    }

    public void setScheduledFuture(ScheduledFuture<?> scheduledFuture) {
        this.scheduledFuture = scheduledFuture;
    }
}