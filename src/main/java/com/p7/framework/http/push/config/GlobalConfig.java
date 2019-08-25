package com.p7.framework.http.push.config;

import java.net.Inet4Address;

/**
 * 初始化推送环境
 *
 * @author Yangzhen
 **/
public class GlobalConfig {

    private static final String PUSH_SERVICE_ID;

    public static final String HTTPS = "https";

    public static final String HTTP = "http";

    public static final String UTF8 = "UTF-8";

    /**
     * 定义推送成功时，第三方返回的状态码
     */
    public static final String SUCCESS_CODE = "0";

    /**
     * 定义第三方接收到数据后要返回Map的json数据，code = 0
     */
    public static final String CODE = "code";
    /**
     * 定义第三方接收到数据后要返回Map的json数据，data = msgId
     */
    public static final String DATA = "data";

    public static final String BATCH_PUSH_HTTP_TIME_OUT = "5000";
    public static final String BATCH_PUSH_HTTPS_TIME_OUT = "5000";
    public static final String SINGLE_PUSH_HTTP_TIME_OUT = "3000";
    public static final String SINGLE_PUSH_HTTPS_TIME_OUT = "3000";

    /**
     * 获取本机主机名
     */
    static {
        String hostName = "";
        try {
            hostName = Inet4Address.getLocalHost().toString().replace(".", "_").replaceAll("/", "_");
        } catch (Exception e) {
            e.printStackTrace();
        }
        PUSH_SERVICE_ID = hostName;
    }

    /**
     * 返回主机名
     *
     * @return
     */
    public static String getPushServiceId() {
        return PUSH_SERVICE_ID;
    }

}
