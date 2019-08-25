package com.p7.framework.http.push.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 推送实体
 *
 * @author Yangzhen
 **/
public class PushModel implements Serializable {

    public PushModel() {
    }

    /**
     * 主键
     */
    private String msgId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 最后一次推送时间
     **/
    private Date lastPushTime;

    /**
     * 推送次数
     **/
    private Integer pushTimes = 0;

    /**
     * 限制推送次数，默认为3，实际以appId配置的推送配置为准
     **/
    private Integer leastRetryTimes = 3;

    /**
     * appId对应的推送配置
     **/
    private String url;

    /**
     * appId
     */
    private Integer appId;

    /**
     * 推送数据，json格式
     */
    private String pushData;

    /**
     * 推送状态码
     */
    private Integer pushState;

    /**
     * 内存队列下标，不需要二次计算
     */
    private Integer queueIndex;

    /**
     * 服务实例产生的服务id
     */
    private String pushServiceId;

    /**
     * appId对应的批量推送的类型，相同appId相同pushType，批量推送时使用同一个任务
     */
    private Integer pushType;

    /**
     * mysql分区使用
     */
    private Date dataTime;

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getLastPushTime() {
        return lastPushTime;
    }

    public void setLastPushTime(Date lastPushTime) {
        this.lastPushTime = lastPushTime;
    }

    public Integer getPushTimes() {
        return pushTimes;
    }

    public void setPushTimes(Integer pushTimes) {
        this.pushTimes = pushTimes;
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

    public Integer getAppId() {
        return appId;
    }

    public void setAppId(Integer appId) {
        this.appId = appId;
    }

    public String getPushData() {
        return pushData;
    }

    public void setPushData(String pushData) {
        this.pushData = pushData;
    }

    public Integer getQueueIndex() {
        return queueIndex;
    }

    public void setQueueIndex(Integer queueIndex) {
        this.queueIndex = queueIndex;
    }

    public String getPushServiceId() {
        return pushServiceId;
    }

    public void setPushServiceId(String pushServiceId) {
        this.pushServiceId = pushServiceId;
    }

    public Date getDataTime() {
        return dataTime;
    }

    public void setDataTime(Date dataTime) {
        this.dataTime = dataTime;
    }

    public Integer getPushState() {
        return pushState;
    }

    public void setPushState(Integer pushState) {
        this.pushState = pushState;
    }

    public Integer getPushType() {
        return pushType;
    }

    public void setPushType(Integer pushType) {
        this.pushType = pushType;
    }
}