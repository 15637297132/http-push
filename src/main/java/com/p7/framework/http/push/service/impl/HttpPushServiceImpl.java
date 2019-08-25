package com.p7.framework.http.push.service.impl;

import com.p7.framework.http.push.manage.BatchPushManager;
import com.p7.framework.http.push.manage.SinglePushManager;
import com.p7.framework.http.push.model.PushConfig;
import com.p7.framework.http.push.model.PushModel;
import com.p7.framework.http.push.service.HttpPushService;
import com.p7.framework.http.push.service.PushConfigService;
import com.p7.framework.http.push.service.PushRecordService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Yangzhen
 **/
@Service("httpPushService")
public class HttpPushServiceImpl implements HttpPushService, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpPushServiceImpl.class);

    @Resource
    private PushRecordService pushRecordService;

    @Resource
    private SinglePushManager singlePushManager;

    @Resource
    private BatchPushManager batchPushManager;

    @Resource
    private PushConfigService pushConfigService;

    private ConcurrentHashMap<String, PushConfig> pushConfigMap = new ConcurrentHashMap<>();

    @Scheduled(cron = "0 0/1 * * * ? ")
    public void initLocalCache() {
        List<PushConfig> list = pushConfigService.getList();
        if (list != null && list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                PushConfig pushConfig = list.get(i);
                String key = pushConfig.getAppId() + ":" + pushConfig.getPushType();
                pushConfigMap.put(key, list.get(i));
            }
        }
        LOGGER.info("initLocalCache success ...");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initLocalCache();
    }

    @Override
    public void singlePushJson(String json, Integer appId, Integer pushType) {
        PushConfig pushConfig = (PushConfig) pushConfigMap.get(appId + ":" + pushType);
        PushModel pushModel = getPushModel(json, appId, pushType, pushConfig);
        if (pushModel != null) {
            singlePushManager.addPushModel(pushModel);
        }
    }

    @Override
    public void delayBatchPushJson(String json, Integer appId, Integer pushType) {

        PushConfig pushConfig = (PushConfig) pushConfigMap.get(appId + ":" + pushType);
        PushModel pushModel = getPushModel(json, appId, pushType, pushConfig);
        if (pushModel != null) {
            batchPushManager.addPushModel(pushModel, pushConfig, "httpPushService");
        }
    }

    public PushModel getPushModel(String json, Integer appId, Integer pushType, PushConfig pushConfig) {
        if (StringUtils.isBlank(json) || appId == null || pushType == null) {
            LOGGER.error("参数错误... json is {} , appId is {} , pushType is {}", json, appId, pushType);
            return null;
        }
        PushModel pushModel = pushRecordService.savePushRecord(json, appId, pushType, pushConfig);
        return pushModel;
    }

}
