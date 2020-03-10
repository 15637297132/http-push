package com.p7.framework.http.push.constant;

/**
 * 数据入库类型
 *
 * @author Yangzhen
 **/
public enum PushStatusEnum {

    /**
     * 推送成功
     */
    PUSH_SUCCESS(0),

    /**
     * 响应数据解析失败
     */
    RESPONSE_DATA_ERROR(1),

    /**
     * 远程调用失败
     */
    REMOTE_CALL_FAILED(2),

    /**
     * 本地错误
     */
    LOCAL_ERROR(3),

    /**
     * 重试限制
     */
    RETRY_LIMITED(4),

    /**
     * 等待推送
     */
    PUSH_WAIT(5);

    private int pushState;

    private PushStatusEnum(int pushState) {
        this.pushState = pushState;
    }

    public int getPushState() {
        return this.pushState;
    }

}
