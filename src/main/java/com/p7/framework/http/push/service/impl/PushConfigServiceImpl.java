package com.p7.framework.http.push.service.impl;


import com.p7.framework.http.push.mapper.PushConfigMapper;
import com.p7.framework.http.push.model.PushConfig;
import com.p7.framework.http.push.service.PushConfigService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Yangzhen
 **/
@Service("pushConfigService")
public class PushConfigServiceImpl implements PushConfigService {

    @Resource
    private PushConfigMapper pushConfigMapper;

    @Override
    public List<PushConfig> getList() {
        return pushConfigMapper.getList();
    }

    @Override
    public PushConfig get(PushConfig param) {
        return pushConfigMapper.get(param);
    }
}
