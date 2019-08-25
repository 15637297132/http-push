package com.p7.framework.http.push.config;

import java.util.Map;

/**
 * 单条推送重试间隔
 *
 * @author Yangzhen
 **/
public class SinglePushRetryConfig {
    /**
     * 推送时间次数map
     * key：次数
     * value：时间间隔
     */
    private Map<Integer, Long> pushParams;

    public Map<Integer, Long> getPushParams() {
        return pushParams;
    }

    public void setPushParams(Map<Integer, Long> pushParams) {
        this.pushParams = pushParams;
    }

    public Integer getMaxPushTime() {
        return pushParams == null ? 0 : pushParams.size();
    }
}
