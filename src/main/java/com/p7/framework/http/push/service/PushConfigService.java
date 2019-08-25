package com.p7.framework.http.push.service;



import com.p7.framework.http.push.model.PushConfig;

import java.util.List;

/**
 * 推送配置
 *
 * @author Yangzhen
 **/
public interface PushConfigService {

    /**
     * 获取所有推送配置
     *
     * @return
     */
    List<PushConfig> getList();

    /**
     * 查询
     *
     * @param param
     * @return
     */
    PushConfig get(PushConfig param);
}
