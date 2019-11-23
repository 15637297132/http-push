package com.p7.framework.http.push.test;


import com.p7.framework.http.push.model.PushModel;
import com.p7.framework.http.push.service.HttpPushService;
import com.p7.framework.http.push.util.AbstractConcurrentControl;

import java.util.*;

/**
 * @author Yangzhen
 **/
public class ConcurrentBatchTask extends AbstractConcurrentControl {

    static Random random = new Random();

    static List<Integer> appId = new ArrayList<>();
    static Map<Integer, Integer> pushTypes = new HashMap<>();

    static {
        appId.add(12301);
//        appId.add(12302);
//        appId.add(12303);
//        appId.add(12304);
        pushTypes.put(12301, 0);
//        pushTypes.put(12301, 1);
//        pushTypes.put(12302, 0);
//        pushTypes.put(12303, 0);
//        pushTypes.put(12304, 0);
    }

    private HttpPushService httpPushService;

    public ConcurrentBatchTask(HttpPushService httpPushService) {
        super(1500);
        this.httpPushService = httpPushService;
    }

    @Override
    protected void code() {
        try {
            PushModel data = data();
            httpPushService.delayBatchPushJson(data.getPushData(), data.getAppId(), data.getPushType());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected <T> T data() {
        PushModel pushModel = new PushModel();
        pushModel.setLastPushTime(new Date());
        String msgId = UUID.randomUUID().toString();
        int i = random.nextInt(3);
        pushModel.setAppId(appId.get(i));
        pushModel.setPushType(pushTypes.get(appId.get(i)));
        String pushData = "{\"data\":{\"openId\":\"56777E6B8D1048429B1C7E945225A8ED\",\"breathRateList\":[],\"deepSleepPercent\":0.58,\"deepSleepDuration\":151,\"asleepDuration\":262,\"score\":33.0,\"beatPercent\":99.0,\"wakeDuration\":0,\"wakePercent\":0.0,\"fallSleepDuration\":1,\"turnOverTimes\":130,\"thirdUserSleepTypes\":[],\"sleepDuration\":262,\"sleepScope\":\"2019-04-17 18:23:59~2019-04-17 22:11:28\",\"breathRate\":14,\"sleepQuality\":\"糟糕\",\"sleepStatusList\":[{\"startTime\":1555467839000,\"status\":4},{\"startTime\":1555472639000,\"status\":2},{\"startTime\":1555473359000,\"status\":3},{\"startTime\":1555473419000,\"status\":4},{\"startTime\":1555474439000,\"status\":7},{\"startTime\":1555474739000,\"status\":5},{\"startTime\":1555474799000,\"status\":4},{\"startTime\":1555475459000,\"status\":6},{\"startTime\":1555476419000,\"status\":2},{\"startTime\":1555476479000,\"status\":4},{\"startTime\":1555476929000,\"status\":3},{\"startTime\":1555478189000,\"status\":4},{\"startTime\":1555479779000,\"status\":7},{\"startTime\":1555480859000,\"status\":6},{\"startTime\":1555480919000,\"status\":4},{\"startTime\":1555481488000,\"status\":6}],\"updateTime\":1555574080000,\"lightSleepDuration\":111,\"turnOverList\":[],\"lightSleepPercent\":0.42,\"heartRate\":61,\"phone\":\"\",\"dataTime\":\"2019-04-18\",\"heartRateList\":[]},\"dataType\":11,\"mac\":\"405EE1900DFA\",\"timestamp\":\"2019-04-18\"}";
        pushModel.setPushData("{\"data\":" + intCounter.getAndIncrement() + "" + "}");
        return (T) pushModel;
    }

    @Override
    public void lock() {

    }
}
