package com.p7.framework.http.push.task;

import com.alibaba.fastjson.JSON;
import com.p7.framework.http.push.config.GlobalConfig;
import com.p7.framework.http.push.constant.PushStatusEnum;
import com.p7.framework.http.push.manage.BatchPushControl;
import com.p7.framework.http.push.manage.ScanDatabaseManager;
import com.p7.framework.http.push.manage.SinglePushControl;
import com.p7.framework.http.push.model.PushModel;
import com.p7.framework.http.push.service.PushRecordService;
import com.p7.framework.http.push.util.HttpClientUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 批处理任务，此任务会一直存在在内存中，有推送数据时，取出当前任务并添加到属性中
 *
 * @author Yangzhen
 **/
public class BatchPushTask implements Runnable, Delayed {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchPushTask.class);

    private Object obj = new Object();
    private volatile ConcurrentHashMap<String, Object> msgIdMap = new ConcurrentHashMap();

    static final float DEFAULT_LOAD_FACTOR = 0.75F;

    /**
     * 单位：ms
     */
    private long nextTaskInterval;

    /**
     * 每个任务限制推送的次数
     */
    private Integer leastRetryTimes;
    /**
     * 执行时间
     */
    private long executeTime;

    /**
     * appId
     */
    private Integer appId;

    /**
     * 一个appId对应一个地址
     * 批任务的推送地址
     */
    private String url;

    /**
     * 容量
     */
    private Integer capacity;

    /**
     * 批任务的推送id
     */
    private String msgId;

    /**
     * 所在队列下标，避免重新计算内存下标
     */
    private Integer queueIndex;

    /**
     * 推送类型
     */
    private Integer pushType;

    /**
     * 推送内容
     */
    private LinkedList<PushModel> dataList = new LinkedList<>();

    /**
     * 推送响应成功标识
     */
    private boolean callSuccess = false;

    /**
     * 千万不要加static
     */
    public volatile boolean ifRun = false;

    /**
     * db第一次扫描延迟时长，单位：ms
     */
    private Integer firstDelayedMs;

    /**
     * db扫描轮询延迟时长，单位：ms
     */
    private Integer delayedCycleMs;

    /**
     * 当pushConfig中的firstDelayedMs，delayedCycleMs更改后，重新添加db扫描任务的标识
     */
    private boolean changeScanStrategy;

    /**
     * 是否销毁数据库扫描的任务，在pushConfig禁用时删除任务
     */
    public volatile boolean destroyed = false;

    private PushRecordService pushRecordService;

    private ScanDatabaseManager scanDatabaseManager;

    private ScanDatabaseTask scanDatabaseTask;

    public BatchPushTask() {
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(executeTime - System.currentTimeMillis(), TimeUnit.SECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        BatchPushTask task = (BatchPushTask) o;
        return executeTime > task.executeTime ? 1 : (executeTime < task.executeTime ? -1 : 0);
    }

    @Override
    public synchronized void run() {
        try {
            /**
             * 推送数据为空时，直接返回
             */
            if (dataList.isEmpty()) {
                return;
            }
            // 发送数据
            String data = getPushData();
            LOGGER.info("appId is {}, pushType is {} , url is {} , 当前推送数据: {} ", appId, pushType, url, data);
            String result = null;
            if (url.startsWith(GlobalConfig.HTTP)) {
                result = HttpClientUtil.httpSendPost(url, data, GlobalConfig.UTF8, GlobalConfig.BATCH_PUSH_HTTP_TIME_OUT, GlobalConfig.BATCH_PUSH_HTTP_TIME_OUT);
            } else if (url.startsWith(GlobalConfig.HTTPS)) {
                result = HttpClientUtil.httpsRequest(url, data, GlobalConfig.UTF8, GlobalConfig.BATCH_PUSH_HTTPS_TIME_OUT, GlobalConfig.BATCH_PUSH_HTTPS_TIME_OUT);
            }
            // 响应结果
            if (StringUtils.isNotBlank(result)) {
                Map<String, Object> resultMap = JSON.parseObject(result, Map.class);
                String resultCode = String.valueOf(resultMap.get(GlobalConfig.CODE));
                String resultData = String.valueOf(resultMap.get(GlobalConfig.DATA));
                if (StringUtils.isNotBlank(resultCode) && GlobalConfig.SUCCESS_CODE.equals(resultCode) && msgId.equals(resultData)) {
                    LOGGER.info("push success msgId is {} , appId is {} , pushType is {} , serviceId is {} , url is {}", msgId, appId, pushType, GlobalConfig.getPushServiceId(), url);
                    pushRecordService.batchDelSuccess(dataList);
                    LOGGER.info("msgId is {} , batch delete records", msgId);
                    callSuccess = true;
                } else {
                    LOGGER.error("resultCode or resultData error , appId is {} , pushType is {} , serviceId is {} , code is {} , msgId is {}", appId, pushType, GlobalConfig.getPushServiceId(), resultCode, resultData);
                    System.out.println("error..." + JSON.toJSONString(dataList));
                    batchUpdateStatus(dataList, PushStatusEnum.RESPONSE_DATA_ERROR);
                }
            } else {
                LOGGER.error("result is null , http request failed , remote server exception . appId is {} , pushType is {} , serviceId is {}", appId, pushType, GlobalConfig.getPushServiceId());
                batchUpdateStatus(dataList, PushStatusEnum.REMOTE_CALL_FAILED);
            }
        } catch (Exception e) {
            LOGGER.error("appId is {} , pushType is {} , task run failed ... error is {}", appId, pushType, e.getMessage());
            batchUpdateStatus(dataList, PushStatusEnum.LOCAL_ERROR);
        } finally {
            if (callSuccess) {
                dataList.clear();
                msgIdMap.clear();
                this.callSuccess = false;
                LOGGER.info("########## appId is {} , pushType is {} , dataList is {} , msgIdMap is {}", appId, pushType, JSON.toJSONString(dataList), JSON.toJSONString(msgIdMap.keys()));
            } else {
                checkPushTimes();
            }
            this.ifRun = false;
            nextLoop();
        }
    }

    /**
     * 添加任务，当任务列表超过阈值时触发任务执行
     *
     * @param pushModel
     */
    public void addData(PushModel pushModel, String from) {
        try {
            // 如果任务正在执行，那么直接返回，避免过多的添加操作阻塞在此
            if (!ifRun) {
                synchronized (this) {
                    // 当前任务列表中已存在时，直接返回
                    if (msgIdMap.containsKey(pushModel.getMsgId())) {
                        return;
                    }
                    int size = dataList.size();
                    // 当任务数量 + 1 大于容量时，添加任务到集合头部，移除队列尾部任务，并且触发推送
                    if ((size + 1) >= capacity) {
                        dataList.addFirst(pushModel);
                        msgIdMap.put(pushModel.getMsgId(), obj);
                        String lastMsgId = null;
                        // 当集合元素数量大于1时，移除尾部任务
                        if (dataList.size() > 1) {
                            PushModel last = dataList.removeLast();
                            msgIdMap.remove(last.getMsgId());
                            lastMsgId = last.getMsgId();
                        }
                        LOGGER.info("任务添加成功 msgId is {} , appId is {} , pushType is {} , serviceId is {} , {} , capacity full , trigger task run , add first {} , remove last {} ", pushModel.getMsgId(), appId, pushType, GlobalConfig.getPushServiceId(), from, pushModel.getMsgId(), lastMsgId);
                        boolean remove = BatchPushControl.removeTaskFromQueue(this);
                        if (remove) {
                            // 这里使用线程池提交任务可能会出现问题
                            this.ifRun = true;
                            this.run();
                        }
                    } else {
                        dataList.addFirst(pushModel);
                        msgIdMap.put(pushModel.getMsgId(), obj);
                        LOGGER.info("任务添加成功 msgId is {} , appId is {} , pushType is {} , serviceId is {} , {} ", pushModel.getMsgId(), appId, pushType, GlobalConfig.getPushServiceId(), from);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("appId is {} ,pushType is {} , addData error, error is {} , from {}", appId, pushType, e.getMessage(), from);
        }
    }

    /**
     * 组装推送数据
     *
     * @return
     */
    private String getPushData() {
        Map<String, Object> pushData = new HashMap<String, Object>(2);
        msgId = UUID.randomUUID().toString();
        pushData.put("msgId", msgId);
        List<Object> data = new ArrayList<>();
        Iterator<PushModel> iterator = this.dataList.iterator();
        while (iterator.hasNext()) {
            PushModel next = iterator.next();
            data.add(JSON.parse(next.getPushData()));
        }
        pushData.put("list", data);
        return JSON.toJSONString(pushData);
    }

    /**
     * 触发数据库扫描
     */
    public void triggerScan() {
        // 如果任务正在执行，那么直接返回，避免过多的添加操作阻塞在此
        if (!ifRun) {
            synchronized (this) {
                int size = msgIdMap.size();
                // 注意，由于数据库操作有时间空隙，因此数据库扫描时，可能会和单条推送的数据重合，即可能有部分数据重复推送
                if (size <= getThreshold()) {
                    Set<String> keys = msgIdMap.keySet();
                    PushModel param = new PushModel();
                    param.setPushTimes(leastRetryTimes);
                    param.setAppId(appId);
                    param.setPushType(pushType);
                    param.setPushServiceId(GlobalConfig.getPushServiceId());
                    List<PushModel> waitingPush = pushRecordService.getWaitTaskBatch(param, capacity - size, keys);
                    if (waitingPush != null && !waitingPush.isEmpty()) {
                        for (int i = 0; (i < capacity) && (i < waitingPush.size()); i++) {
                            if ((!msgIdMap.containsKey(waitingPush.get(i).getMsgId())) && (!SinglePushControl.msgIdMap.containsKey(waitingPush.get(i).getMsgId()))) {
                                addData(waitingPush.get(i), "scan database");
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 批量更新状态
     *
     * @param list
     * @param pushStatusEnum
     */
    private void batchUpdateStatus(List<PushModel> list, PushStatusEnum pushStatusEnum) {
        PushModel param = new PushModel();
        param.setPushState(pushStatusEnum.getPushState());
        param.setLastPushTime(new Date(executeTime));
        pushRecordService.batchUpdatePushStatus(list, param);
    }

    /**
     * 检查超过阈值的任务并移除任务列表
     */
    private void checkPushTimes() {
        Iterator<PushModel> iterator = this.dataList.iterator();
        while (iterator.hasNext()) {
            PushModel next = iterator.next();
            next.setPushTimes(next.getPushTimes() + 1);
            if (next.getPushTimes() >= leastRetryTimes) {
                iterator.remove();
                msgIdMap.remove(next.getMsgId());
            }
        }
    }

    /**
     * 下次任务执行的时间
     */
    private void nextLoop() {
        this.executeTime = System.currentTimeMillis() + nextTaskInterval;
        BatchPushControl.addTaskToQueue(this);
        LOGGER.info("appId is {} , pushType is {} , next execute time is {}", appId, pushType, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(executeTime));
    }

    /**
     * 销毁任务
     */
    public void destroyTask() {
        if (scanDatabaseTask != null) {
            scanDatabaseManager.removeTask(scanDatabaseTask, scanDatabaseTask.getScheduledFuture());
            this.scanDatabaseTask = null;
        }
    }

    public long getNextTaskInterval() {
        return nextTaskInterval;
    }

    public void setNextTaskInterval(long nextTaskInterval) {
        this.nextTaskInterval = nextTaskInterval;
    }

    public Integer getLeastRetryTimes() {
        return leastRetryTimes;
    }

    public void setLeastRetryTimes(Integer leastRetryTimes) {
        this.leastRetryTimes = leastRetryTimes;
    }

    public long getExecuteTime() {
        return executeTime;
    }

    public void setExecuteTime(long executeTime) {
        this.executeTime = executeTime;
    }

    public Integer getAppId() {
        return appId;
    }

    public void setAppId(Integer appId) {
        this.appId = appId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public PushRecordService getPushRecordService() {
        return pushRecordService;
    }

    public void setPushRecordService(PushRecordService pushRecordService) {
        this.pushRecordService = pushRecordService;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getQueueIndex() {
        return queueIndex;
    }

    public void setQueueIndex(Integer queueIndex) {
        this.queueIndex = queueIndex;
    }

    public Integer getPushType() {
        return pushType;
    }

    public void setPushType(Integer pushType) {
        this.pushType = pushType;
    }

    public Integer getFirstDelayedMs() {
        return firstDelayedMs;
    }

    public void setFirstDelayedMs(Integer firstDelayedMs) {
        this.firstDelayedMs = firstDelayedMs;
    }

    public Integer getDelayedCycleMs() {
        return delayedCycleMs;
    }

    public void setDelayedCycleMs(Integer delayedCycleMs) {
        this.delayedCycleMs = delayedCycleMs;
    }

    public boolean getChangeScanStrategy() {
        return changeScanStrategy;
    }

    public void setChangeScanStrategy(boolean changeScanStrategy) {
        this.changeScanStrategy = changeScanStrategy;
    }

    public int getThreshold() {
        return this.getCapacity() - (int) (this.getCapacity() * DEFAULT_LOAD_FACTOR);
    }

    public ScanDatabaseManager getScanDatabaseManager() {
        return scanDatabaseManager;
    }

    public void setScanDatabaseManager(ScanDatabaseManager scanDatabaseManager) {
        this.scanDatabaseManager = scanDatabaseManager;
    }

    public ScanDatabaseTask getScanDatabaseTask() {
        return scanDatabaseTask;
    }

    public void setScanDatabaseTask(ScanDatabaseTask scanDatabaseTask) {
        this.scanDatabaseTask = scanDatabaseTask;
    }
}
