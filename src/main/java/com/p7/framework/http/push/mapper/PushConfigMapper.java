package com.p7.framework.http.push.mapper;

import com.p7.framework.http.push.model.PushConfig;

import java.util.List;

/**
 * 推送配置
 *
 * @author Yangzhen
 **/
public interface PushConfigMapper {

    /**
     * 获取所有推送配置
     *
     * @return
     */
    List<PushConfig> getList();

    /**
     * 根据appId获取推送配置
     *
     * @param param
     * @return
     */
    PushConfig get(PushConfig param);
}
