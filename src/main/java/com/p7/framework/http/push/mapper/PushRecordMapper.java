package com.p7.framework.http.push.mapper;

import com.p7.framework.http.push.model.PushModel;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

/**
 * @author Yangzhen
 **/
public interface PushRecordMapper {

    /**
     * 保存推送记录
     * @param pushModel
     */
    void savePushRecord(PushModel pushModel);

    /**
     * 保存推送记录，但没有配置推送配置
     * @param pushModel
     */
    void savePushRecordNoConfig(PushModel pushModel);

    /**
     * 更新推送状态
     * @param pushModel
     */
    void updatePushStatus(PushModel pushModel);

    /**
     * 获取待发送的数据
     *
     * @param pushModel
     * @param num
     * @param keys
     * @return
     */
    List<PushModel> getWaitTaskBatch(@Param("pushModel") PushModel pushModel, @Param("num") Integer num, @Param("keys") Set<String> keys);

    /**
     * 批量更新推送状态
     * @param pushModelList
     * @param param
     */
    void updatePushStatusBatch(@Param("pushModelList") List<PushModel> pushModelList, @Param("param") PushModel param);

    /**
     * 批量删除
     *
     * @param keys
     */
    void batchDelSuccess(@Param("keys") Set<String> keys);
}
