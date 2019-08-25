package com.p7.framework.http.push.manage;

import com.p7.framework.http.push.model.PushConfig;
import com.p7.framework.http.push.model.PushModel;
import com.p7.framework.http.push.service.PushConfigService;
import com.p7.framework.http.push.service.PushRecordService;
import com.p7.framework.http.push.task.BatchPushTask;
import com.p7.framework.http.push.task.ScanDatabaseTask;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 添加推送到批处理任务队列中
 *
 * @author Yangzhen
 **/
@Component
public class BatchPushManager implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchPushManager.class);

    @Resource
    private PushConfigService pushConfigService;

    @Resource
    private PushRecordService pushRecordService;

    @Resource
    private ScanDatabaseManager scanDatabaseManager;

    @Override
    public synchronized void afterPropertiesSet() throws Exception {
        List<PushConfig> pushConfigList = pushConfigService.getList();
        if (pushConfigList != null && !pushConfigList.isEmpty()) {
            BatchPushTask batchPushTask = null;
            for (PushConfig pushConfig : pushConfigList) {
                // 初始化时只初始化可用的
                if (pushConfig.getEnabled() == 0) {
                    batchPushTask = initBatchPushTask(pushConfig);
                    BatchPushControl.taskContainer.put(pushConfig.getAppId() + ":" + pushConfig.getPushType(), batchPushTask);
                    BatchPushControl.addTaskToQueue(batchPushTask);
                    ScanDatabaseTask scanDatabaseTask = new ScanDatabaseTask(batchPushTask, scanDatabaseManager);
                    batchPushTask.setScanDatabaseTask(scanDatabaseTask);
                    scanDatabaseManager.addTask(scanDatabaseTask);
                }
            }
        }
    }

    public BatchPushTask initBatchPushTask(PushConfig pushConfig) {
        BatchPushTask batchPushTask = new BatchPushTask();
        batchPushTask.setAppId(pushConfig.getAppId());
        batchPushTask.setUrl(pushConfig.getUrl());
        Date date = new Date();
        batchPushTask.setExecuteTime(date.getTime() + pushConfig.getNextTaskInterval());
        batchPushTask.setNextTaskInterval(pushConfig.getNextTaskInterval());
        batchPushTask.setPushRecordService(pushRecordService);
        batchPushTask.setScanDatabaseManager(scanDatabaseManager);
        batchPushTask.setLeastRetryTimes(pushConfig.getLeastRetryTimes());
        batchPushTask.setCapacity(pushConfig.getCapacity());
        batchPushTask.setPushType(pushConfig.getPushType());
        batchPushTask.setFirstDelayedMs(pushConfig.getFirstDelayedMs());
        batchPushTask.setDelayedCycleMs(pushConfig.getDelayedCycleMs());
        return batchPushTask;
    }

    /**
     * 添加任务到PushTaskBatch
     *
     * @param pushModel  推送实体
     * @param pushConfig 推送配置，不可能为null
     * @param from       来源，因为是多线程，在调试时，用来标志是数据库扫描的还是直接调用服务的
     */
    public void addPushModel(PushModel pushModel, PushConfig pushConfig, String from) {
        BatchPushTask batchPushTask = BatchPushControl.taskContainer.get(pushModel.getAppId() + ":" + pushConfig.getPushType());
        if (batchPushTask == null) {
            synchronized (this) {
                batchPushTask = BatchPushControl.taskContainer.get(pushModel.getAppId());
                if (batchPushTask == null) {
                    batchPushTask = initBatchPushTask(pushConfig);
                    BatchPushControl.taskContainer.put(batchPushTask.getAppId() + ":" + batchPushTask.getPushType(), batchPushTask);
                    if (pushConfig.getEnabled() == 0) {
                        batchPushTask.addData(pushModel, from);
                        BatchPushControl.addTaskToQueue(batchPushTask);
                        ScanDatabaseTask scanDatabaseTask = new ScanDatabaseTask(batchPushTask, scanDatabaseManager);
                        batchPushTask.setScanDatabaseTask(scanDatabaseTask);
                        scanDatabaseManager.addTask(scanDatabaseTask);
                        LOGGER.info("push config enabled , appId is {} , pushType is {} , init scan task.", pushConfig.getAppId(), pushConfig.getPushType());
                    }
                } else {
                    // 如果出现这种情况，那么数据不添加到内存中，等待下一次扫描数据库时再加入其中
                    if (pushConfig.getEnabled() == 1) {
                        batchPushTask.destroyed = true;
                        BatchPushControl.removeTaskFromQueue(batchPushTask);
                        batchPushTask.destroyTask();
                        LOGGER.info("push config disabled , appId is {} , pushType is {} , destroy scan task.", pushConfig.getAppId(), pushConfig.getPushType());
                    }
                }
            }
        } else {
            synchronized (batchPushTask) {
                // 已经销毁任务
                if (batchPushTask.destroyed) {
                    // 推送配置被禁用
                    if (pushConfig.getEnabled() == 1) {
                        LOGGER.info("appId is {} , pushType is {} , task destroyed", pushConfig.getAppId(), pushConfig.getPushType());
                        return;
                    }
                    // 推送配置可用
                    else {
                        batchPushTask.destroyed = false;
                        batchPushTask.addData(pushModel, from);
                        BatchPushControl.addTaskToQueue(batchPushTask);
                        ScanDatabaseTask scanDatabaseTask = new ScanDatabaseTask(batchPushTask, scanDatabaseManager);
                        batchPushTask.setScanDatabaseTask(scanDatabaseTask);
                        scanDatabaseManager.addTask(scanDatabaseTask);
                        LOGGER.info("push config enabled , appId is {} , pushType is {} , init scan task..", pushConfig.getAppId(), pushConfig.getPushType());
                    }
                }
                // 任务未被销毁
                else {
                    // 推送配置被禁用
                    if (pushConfig.getEnabled() == 1) {
                        batchPushTask.destroyed = true;
                        BatchPushControl.removeTaskFromQueue(batchPushTask);
                        batchPushTask.destroyTask();
                        LOGGER.info("push config disabled , appId is {} , pushType is {} , destroy scan task..", pushConfig.getAppId(), pushConfig.getPushType());
                    } else {
                        // 此三个参数都是数据库可配置参数，因此每次添加任务时重新设置值
                        batchPushTask.setCapacity(pushConfig.getCapacity());
                        batchPushTask.setUrl(pushConfig.getUrl());
                        batchPushTask.setNextTaskInterval(pushConfig.getNextTaskInterval());
                        if (!batchPushTask.getPushType().equals(pushConfig.getPushType())) {
                            batchPushTask.setPushType(pushConfig.getPushType());
                            batchPushTask.setQueueIndex(null);
                        }
                        if (!(batchPushTask.getFirstDelayedMs().equals(pushConfig.getFirstDelayedMs())) || !(batchPushTask.getDelayedCycleMs().equals(pushConfig.getDelayedCycleMs()))) {
                            batchPushTask.setFirstDelayedMs(pushConfig.getFirstDelayedMs());
                            batchPushTask.setDelayedCycleMs(pushConfig.getDelayedCycleMs());
                            batchPushTask.setChangeScanStrategy(true);
                        }
                        batchPushTask.addData(pushModel, from);
                    }
                }
            }
        }
    }
}
