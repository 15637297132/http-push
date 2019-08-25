package com.p7.framework.http.push.service;

/**
 * 推送服务
 *
 * @author Yangzhen
 **/
public interface HttpPushService {

    /**
     * 单条推送数据
     *
     * @param json
     * @param appId
     * @param pushType
     */
    void singlePushJson(String json, Integer appId, Integer pushType);

    /**
     * 批量推送数据
     *
     * @param json
     * @param appId
     * @param pushType
     */
    void delayBatchPushJson(String json, Integer appId, Integer pushType);
}
