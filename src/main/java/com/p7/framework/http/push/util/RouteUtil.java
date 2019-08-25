package com.p7.framework.http.push.util;

import org.apache.commons.lang3.StringUtils;

/**
 * 路由
 *
 * @author Yangzhen
 **/
public class RouteUtil {

    /**
     * 计算推送消息所在延时队列的下标，注意：延时队列集合元素个数必须要为2的次幂
     *
     * @param key
     * @return
     */
    public static Integer route(String key, int factor) {
        if (StringUtils.isBlank(key)) {
            return null;
        }
        int h;
        int hash = (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
        factor = factor - 1;
        int index = factor & hash;
        return index;
    }
}
