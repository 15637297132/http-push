package com.p7.framework.http.push.constant;

/**
 * 数据入库类型
 *
 * @author Yangzhen
 **/
public enum PushStatusEnum {

    PUSH_SUCCESS(0),

    RESPONSE_DATA_ERROR(1),

    REMOTE_CALL_FAILED(2),

    LOCAL_ERROR(3),

    RETRY_LIMITED(4),

    PUSH_WAIT(5);

    private int pushState;

    private PushStatusEnum(int pushState) {
        this.pushState = pushState;
    }

    public int getPushState() {
        return this.pushState;
    }

}
