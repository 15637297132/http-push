package com.p7.framework.http.push.manage;

import com.p7.framework.http.push.model.PushModel;
import com.p7.framework.http.push.service.PushRecordService;
import com.p7.framework.http.push.task.SinglePushTask;
import com.p7.framework.http.push.util.RouteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 单条推送控制
 *
 * @author Yangzhen
 **/
public class SinglePushControl {

    private static final Logger LOGGER = LoggerFactory.getLogger(SinglePushControl.class);

    /**
     * 维护所有单任务的msgId
     * 1.delayQueues队列满时移除
     * 2.http请求结束后，无论成功与否，都要在重试之前移除msgId，为了减少扫描db和内存执行的冲突时间，执行完pushPersist.updatePushStatus后再移除，并且在pushQueue.addPushModel之前执行
     * 3.推满五次的移除
     */
    public static final ConcurrentHashMap<String, Object> msgIdMap = new ConcurrentHashMap<String, Object>();
    public static List<DelayQueue<SinglePushTask>> delayQueues = new ArrayList(2);
    public static DelayQueue<SinglePushTask> tasks0 = new DelayQueue<SinglePushTask>();
    public static DelayQueue<SinglePushTask> tasks1 = new DelayQueue<SinglePushTask>();

    public static int task_threshold = Integer.MAX_VALUE >> 15;

    static {
        delayQueues.add(tasks0);
        delayQueues.add(tasks1);
    }

    /**
     * 内存计数器，每成功添加一个任务时加1，执行run方法时减1.
     */
    public static AtomicInteger TASK_COUNTER = new AtomicInteger(0);

    /**
     * 阈值，用来定义内存中任务的数量，因为DelayQueue是无界队列，避免OOM
     * 定于阈值为65535，这里不需要考虑此方法的线程安全，因为内存远远大于65535 * 4所占内存
     *
     * @return
     */
    public static boolean maxTaskSize() {
        return TASK_COUNTER.intValue() >= task_threshold;
    }

    /**
     * 获取延迟队列
     *
     * @param index
     * @return
     */
    public static DelayQueue<SinglePushTask> get(int index) {
        return delayQueues.get(index);
    }

    /**
     * 添加任务到队列
     *
     * @param pushModel
     */
    public static void addTaskToQueue(PushModel pushModel, SinglePushManager singlePushManager, PushRecordService pushRecordService) {
        Integer index = pushModel.getQueueIndex();
        if (index == null) {
            index = RouteUtil.route(pushModel.getMsgId(), delayQueues.size());
        }
        if (index == null) {
            LOGGER.error("addTaskToQueue pushModel's msgId is null , route queue index failed... ");
            return;
        }
        // 自增
        TASK_COUNTER.incrementAndGet();
        DelayQueue<SinglePushTask> singlePushTasks = get(index);
        singlePushTasks.put(new SinglePushTask(pushModel, singlePushManager, pushRecordService));
        LOGGER.info("route success , msgId is {} , queue index is {}", pushModel.getMsgId(), index);
    }

}
