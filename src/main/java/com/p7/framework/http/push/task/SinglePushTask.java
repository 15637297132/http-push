package com.p7.framework.http.push.task;

import com.alibaba.fastjson.JSON;
import com.p7.framework.http.push.config.GlobalConfig;
import com.p7.framework.http.push.constant.PushStatusEnum;
import com.p7.framework.http.push.manage.SinglePushControl;
import com.p7.framework.http.push.manage.SinglePushManager;
import com.p7.framework.http.push.model.PushModel;
import com.p7.framework.http.push.service.PushRecordService;
import com.p7.framework.http.push.util.HttpClientUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 推送单次任务
 *
 * @author Yangzhen
 **/
public class SinglePushTask implements Runnable, Delayed {

    private static final Logger LOGGER = LoggerFactory.getLogger(SinglePushTask.class);

    /**
     * 执行时间
     */
    private long executeTime;

    /**
     * 推送实体
     */
    private PushModel pushModel;

    /**
     * 推送队列
     */
    private SinglePushManager singlePushManager;

    /**
     * 持久化
     */
    private PushRecordService pushRecordService;

    public SinglePushTask() {
    }

    public SinglePushTask(PushModel pushModel, SinglePushManager singlePushManager, PushRecordService pushRecordService) {
        super();
        this.pushModel = pushModel;
        this.singlePushManager = singlePushManager;
        this.pushRecordService = pushRecordService;
        this.executeTime = getExecuteTime(pushModel.getLastPushTime().getTime());
    }

    /**
     * 创建PushTask时调用
     *
     * @param lastPushTime
     * @return
     */
    private long getExecuteTime(long lastPushTime) {
        // 执行时间
        LOGGER.info("executeTime is {} , msgId is {}", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(lastPushTime), pushModel.getMsgId());
        return lastPushTime;
    }

    /**
     * 执行时间小的在队列头
     *
     * @param o
     * @return
     */
    @Override
    public int compareTo(Delayed o) {
        SinglePushTask task = (SinglePushTask) o;
        return executeTime > task.executeTime ? 1 : (executeTime < task.executeTime ? -1 : 0);
    }

    /**
     * 延迟策略
     *
     * @param unit
     * @return
     */
    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(executeTime - System.currentTimeMillis(), TimeUnit.SECONDS);
    }

    @Override
    public void run() {

        if (pushModel == null || pushModel.getPushTimes() == null || pushModel.getUrl() == null || pushModel.getMsgId() == null) {
            LOGGER.error("task run , param error ");
            return;
        }

        SinglePushControl.TASK_COUNTER.decrementAndGet();
        long pushStartLong = System.currentTimeMillis();
        // 得到当前通知对象的通知次数
        Integer pushTimes = pushModel.getPushTimes();
        String url = pushModel.getUrl();
        String msgId = pushModel.getMsgId();
        LOGGER.info("开始执行时间 is {} , msgId is {}", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(pushStartLong), msgId);

        try {
            LOGGER.info("url is {} ; MsgId is {} ; PushTimes is {}", url, msgId, pushModel.getPushTimes());
            String data = getPushData();
            System.out.println("当前推送数据：" + data);
            String result = null;
            if (url.startsWith(GlobalConfig.HTTP)) {
                result = HttpClientUtil.httpSendPost(url, data, GlobalConfig.UTF8, GlobalConfig.SINGLE_PUSH_HTTP_TIME_OUT, GlobalConfig.SINGLE_PUSH_HTTP_TIME_OUT);
            } else if (url.startsWith(GlobalConfig.HTTPS)) {
                result = HttpClientUtil.httpsRequest(url, data, GlobalConfig.UTF8, GlobalConfig.SINGLE_PUSH_HTTPS_TIME_OUT, GlobalConfig.SINGLE_PUSH_HTTPS_TIME_OUT);
            }
            pushModel.setPushTimes(pushTimes + 1);
            if (StringUtils.isNotBlank(result)) {
                Map<String, Object> resultMap = JSON.parseObject(result, Map.class);

                String resultCode = String.valueOf(resultMap.get(GlobalConfig.CODE));
                String resultData = String.valueOf(resultMap.get(GlobalConfig.DATA));
                LOGGER.info("resultCode：" + resultCode + "--" + "resultData:" + resultData);
                if (StringUtils.isNotBlank(resultCode) && GlobalConfig.SUCCESS_CODE.equals(resultCode) && msgId.equals(resultData)) {
                    LOGGER.info("push [msgId:" + msgId + "] success , costs " + (System.currentTimeMillis() - pushStartLong) + "  ms");
                    pushRecordService.updatePushStatus(pushModel.getMsgId(), pushModel.getPushTimes(), PushStatusEnum.PUSH_SUCCESS, pushModel);
                    SinglePushControl.msgIdMap.remove(msgId);
                } else {
                    LOGGER.error("resultCode error or resultData error. push [msgId is {}] failed , costs is {} ms , nextPushTime is {}", msgId, (System.currentTimeMillis() - pushStartLong));
                    pushRecordService.updatePushStatus(pushModel.getMsgId(), pushModel.getPushTimes(), PushStatusEnum.RESPONSE_DATA_ERROR, pushModel);
                    SinglePushControl.msgIdMap.remove(msgId);
                    singlePushManager.addPushModel(pushModel);
                }
            } else {
                LOGGER.error("result is null. push [msgId is {}] failed , costs is {} ms", msgId, (System.currentTimeMillis() - pushStartLong));
                pushRecordService.updatePushStatus(msgId, pushModel.getPushTimes(), PushStatusEnum.RESPONSE_DATA_ERROR, pushModel);
                SinglePushControl.msgIdMap.remove(msgId);
                singlePushManager.addPushModel(pushModel);
            }

        } catch (Exception e) {
            LOGGER.error("task run failed ... error is {}", e.getMessage());
            SinglePushControl.msgIdMap.remove(msgId);
            singlePushManager.addPushModel(pushModel);
        }
    }

    /**
     * 获取推送数据
     *
     * @return
     */
    public String getPushData() {
        Map<String, Object> pushData = new HashMap<String, Object>(2);
        pushData.put("msgId", pushModel.getMsgId());
        List<Object> data = new ArrayList<>(1);
        data.add(JSON.parse(pushModel.getPushData()));
        pushData.put("list", data);
        return JSON.toJSONString(pushData);
    }
}
