package com.p7.framework.http.push.service;


import com.p7.framework.http.push.constant.PushStatusEnum;
import com.p7.framework.http.push.model.PushConfig;
import com.p7.framework.http.push.model.PushModel;

import java.util.List;
import java.util.Set;

/**
 * 推送持久化
 *
 * @author Yangzhen
 **/
public interface PushRecordService {

    /**
     * 保存推送记录
     *
     * @param json  json数据
     * @param appId appId
     * @return
     */
    public PushModel savePushRecord(String json, Integer appId, Integer pushType, PushConfig pushConfig);


    /**
     * 更新推送状态
     *
     * @param msgId
     * @param pushTimes
     * @param pushStatus
     * @param pushModel
     */
    public void updatePushStatus(String msgId, int pushTimes, PushStatusEnum pushStatus, PushModel pushModel);

    /**
     * 批量更新推送状态
     *
     * @param pushModelList
     * @param param
     */
    public void batchUpdatePushStatus(List<PushModel> pushModelList, PushModel param);

    /**
     * 批量查询
     *
     * @param pushModel
     * @param num
     * @return
     */
    public List<PushModel> getWaitTaskBatch(PushModel pushModel, Integer num, Set<String> keys);

    /**
     * 批量删除
     *
     * @param pushModelList
     */
    void batchDelSuccess(List<PushModel> pushModelList);

}
