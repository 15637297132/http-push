package com.p7.framework.http.push.manage;

import com.p7.framework.http.push.config.GlobalConfig;
import com.p7.framework.http.push.config.SinglePushRetryConfig;
import com.p7.framework.http.push.constant.PushStatusEnum;
import com.p7.framework.http.push.model.PushModel;
import com.p7.framework.http.push.service.PushRecordService;
import com.p7.framework.http.push.util.RouteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * 添加任务到队列
 *
 * @author Yangzhen
 **/
@Component
public class SinglePushManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SinglePushManager.class);

    @Resource
    private SinglePushRetryConfig singlePushRetryConfig;

    @Resource
    private PushRecordService pushRecordService;

    /**
     * 将传过来的对象进行通知次数判断，之后决定是否放在任务队列中
     *
     * @param pushModel
     * @throws Exception
     */
    public void addPushModel(PushModel pushModel) {

        if (pushModel == null) {
            return;
        }

        boolean maxTaskSize = SinglePushControl.maxTaskSize();

        if (maxTaskSize) {
            LOGGER.error("任务队列已满...");
            return;
        }

        /**
         * 实测数据时代码不同点：synchronized、UUID
         * 除了以上两点，其他均相同，包括调用 PushManager#msgIdMap#remove;
         * 1000条数据实测
         * 有synchronized
         * 1个队列：04:57.992~05:51.458，约54s
         * 2个队列：06:57.360~07:25.219，约30s
         * 4个队列：08:15.281~08:29.140，约15s
         * 无synchronized
         * 4个队列：13:17.132~13:31.225，约15s
         */
        synchronized (SinglePushControl.msgIdMap) {
            Object obj = SinglePushControl.msgIdMap.putIfAbsent(pushModel.getMsgId(), new Object());
            if (obj != null) {
                LOGGER.info("msgId is {} 任务已在执行...", pushModel.getMsgId());
                return;
            } else {
                SinglePushControl.msgIdMap.put(pushModel.getMsgId(), new Object());
            }
        }

        pushModel.setPushServiceId(GlobalConfig.getPushServiceId());

        /**
         * 推送次数
         */
        Integer pushTimes = pushModel.getPushTimes();
        Integer maxPushTime = singlePushRetryConfig.getMaxPushTime();
        maxPushTime = (maxPushTime == null ? 0 : maxPushTime);

        /**
         * 无论上次的执行时间是多少，都设置当前时间，根据推送次数延时响应的时间
         */
        pushModel.setLastPushTime(new Date());
        long time = pushModel.getLastPushTime().getTime();
        Map<Integer, Long> timeMap = singlePushRetryConfig.getPushParams();
        if (pushTimes < maxPushTime) {
            Integer nextKey = pushTimes + 1;
            Long next = timeMap.get(nextKey);
            if (next != null) {
                time += next;
                Date date = new Date(time);
                LOGGER.info("addPushModel next push time is {} , msgId is {}", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date), pushModel.getMsgId());
                pushModel.setLastPushTime(date);

                Integer index = pushModel.getQueueIndex();
                if (index == null && (index = RouteUtil.route(pushModel.getMsgId(), SinglePushControl.delayQueues.size())) == null) {
                    LOGGER.error("pushModel's msgId is null , route queue index failed... ");
                    return;
                }
                pushModel.setQueueIndex(index);
                SinglePushControl.addTaskToQueue(pushModel, this, pushRecordService);
            }
        } else {
            LOGGER.error("retry 5 times, all failed , msgId is {} , appId is {} ", pushModel.getMsgId(), pushModel.getAppId());
            try {
                // 持久化到数据库中
                pushRecordService.updatePushStatus(pushModel.getMsgId(), pushModel.getPushTimes(), PushStatusEnum.RETRY_LIMITED, pushModel);
                SinglePushControl.msgIdMap.remove(pushModel.getMsgId());
            } catch (Exception e) {
                SinglePushControl.msgIdMap.remove(pushModel.getMsgId());
            }
        }
    }
}
