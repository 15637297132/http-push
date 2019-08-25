package com.p7.framework.http.push.model;

import java.io.Serializable;

/**
 * 推送配置
 *
 * @author Yangzhen
 **/
public class PushConfig implements Serializable {

    /**
     * id
     */
    private Integer pushConfigId;

    /**
     * appId
     */
    private Integer appId;

    /**
     * 批量推送任务的时间间隔
     */
    private long nextTaskInterval;

    /**
     * 至少推送次数
     */
    private Integer leastRetryTimes;

    /**
     * 推送地址
     */
    private String url;

    /**
     * 批量推送时的容量
     */
    private Integer capacity;

    /**
     * 推送类型，类型相同的使用同一个任务
     */
    private Integer pushType;

    /**
     * 数据库扫描，第一次延迟ms后执行
     */
    private Integer firstDelayedMs;

    /**
     * 数据库扫描，周期ms后执行
     */
    private Integer delayedCycleMs;

    /**
     * 0：启用，1：禁用
     */
    private Integer enabled;

    public Integer getAppId() {
        return appId;
    }

    public void setAppId(Integer appId) {
        this.appId = appId;
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getPushConfigId() {
        return pushConfigId;
    }

    public void setPushConfigId(Integer pushConfigId) {
        this.pushConfigId = pushConfigId;
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

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }
}
