package com.p7.framework.http.push.service.impl;

import com.p7.framework.http.push.config.GlobalConfig;
import com.p7.framework.http.push.constant.PushStatusEnum;
import com.p7.framework.http.push.mapper.PushRecordMapper;
import com.p7.framework.http.push.model.PushConfig;
import com.p7.framework.http.push.model.PushModel;
import com.p7.framework.http.push.service.PushRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * 推送持久化
 *
 * @author Yangzhen
 **/
@Service("pushRecordService")
public class PushRecordServiceImpl implements PushRecordService {

    private static final Logger logger = LoggerFactory.getLogger(PushRecordServiceImpl.class);

    @Resource
    private PushRecordMapper pushRecordMapper;

    @Override
    public PushModel savePushRecord(String json, Integer appId, Integer pushType, PushConfig pushConfig) {

        PushModel pushModel = new PushModel();
        Date pushDate = new Date();
        pushModel.setLastPushTime(pushDate);
        String msgId = UUID.randomUUID().toString();
        pushModel.setMsgId(msgId);
        pushModel.setAppId(appId);
        pushModel.setCreateTime(pushDate);
        pushModel.setPushServiceId(GlobalConfig.getPushServiceId());
        pushModel.setPushState(PushStatusEnum.PUSH_WAIT.getPushState());
        pushModel.setPushData(json);
        pushModel.setDataTime(pushDate);
        pushModel.setPushType(pushType);

        if (pushConfig == null) {
            logger.error("push config not exists , appId is {} , pushType is {} , saved in no config table");
            pushRecordMapper.savePushRecordNoConfig(pushModel);
            return null;
        }
        pushModel.setLeastRetryTimes(pushConfig.getLeastRetryTimes());
        pushModel.setUrl(pushConfig.getUrl());
        pushRecordMapper.savePushRecord(pushModel);
        return pushModel;
    }

    @Override
    public void updatePushStatus(String msgId, int pushTimes, PushStatusEnum pushStatus, PushModel pushModel) {
        pushModel.setPushTimes(pushTimes);
        pushModel.setPushState(pushStatus.getPushState());
        pushModel.setLastPushTime(new Date());
        pushRecordMapper.updatePushStatus(pushModel);
    }

    @Override
    public void batchUpdatePushStatus(List<PushModel> pushModelList, PushModel param) {
        pushRecordMapper.updatePushStatusBatch(pushModelList, param);
    }

    @Override
    public List<PushModel> getWaitTaskBatch(PushModel pushModel, Integer num, Set<String> keys) {
        List<PushModel> tasks = pushRecordMapper.getWaitTaskBatch(pushModel, num,keys);
        return tasks;
    }

    @Override
    public void batchDelSuccess(List<PushModel> pushModelList) {
        if (pushModelList == null || pushModelList.isEmpty()) {
            logger.error("pushModelList is null or is empty");
        } else {
            Set<String> keys = new HashSet<>(pushModelList.size());
            for (int i = 0; i < pushModelList.size(); i++) {
                keys.add(pushModelList.get(i).getMsgId());
            }
            pushRecordMapper.batchDelSuccess(keys);
        }
    }
}
