package com.p7.framework.http.push.manage;

import com.p7.framework.http.push.task.BatchPushTask;
import com.p7.framework.http.push.util.RouteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;

/**
 * 推送内存管理，注意DelayQueue队列不能使用take，否则只会第一次有效
 *
 * @author Yangzhen
 **/
public class BatchPushControl {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchPushControl.class);

    /**
     * appId对应的BatchPushTask
     */
    public static ConcurrentHashMap<String, BatchPushTask> taskContainer = new ConcurrentHashMap();
    /**
     * 推送队列
     */
    private static DelayQueue<BatchPushTask> taskQueue0 = new DelayQueue<BatchPushTask>();
    private static DelayQueue<BatchPushTask> taskQueue1 = new DelayQueue<BatchPushTask>();
    private static DelayQueue<BatchPushTask> taskQueue2 = new DelayQueue<BatchPushTask>();
    private static DelayQueue<BatchPushTask> taskQueue3 = new DelayQueue<BatchPushTask>();

    public static List<DelayQueue<BatchPushTask>> taskQueues = new ArrayList(4);

    static {
        taskQueues.add(taskQueue0);
        taskQueues.add(taskQueue1);
        taskQueues.add(taskQueue2);
        taskQueues.add(taskQueue3);
    }

    /**
     * 添加任务到队列
     *
     * @param batchPushTask
     */
    public static void addTaskToQueue(BatchPushTask batchPushTask) {
        Integer queueIndex = batchPushTask.getQueueIndex();
        if (queueIndex == null) {
            queueIndex = RouteUtil.route(batchPushTask.getAppId() + ":" + batchPushTask.getPushType(), taskQueues.size());
        }
        batchPushTask.setQueueIndex(queueIndex);
        DelayQueue<BatchPushTask> taskQueue = taskQueues.get(queueIndex);
        taskQueue.put(batchPushTask);
        LOGGER.info("route success , appId is {} , pushType is {} , queue index is {}", batchPushTask.getAppId(), batchPushTask.getPushType(), queueIndex);
    }

    public static boolean removeTaskFromQueue(BatchPushTask batchPushTask) {
        if (batchPushTask.getQueueIndex() == null) {
            return Boolean.FALSE;
        }
        return BatchPushControl.taskQueues.get(batchPushTask.getQueueIndex()).remove(batchPushTask);
    }

    public static int size(BatchPushTask batchPushTask) {
        return BatchPushControl.taskQueues.get(batchPushTask.getQueueIndex()).size();
    }
}
