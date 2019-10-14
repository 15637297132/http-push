package com.p7.framework.http.push;

import com.p7.framework.http.push.config.GlobalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author Yangzhen
 * @Description
 * @date 2019-09-27 14:51
 **/
@Component
public class SystemStartedEvent implements ApplicationListener<ContextStartedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemStartedEvent.class);

    public static volatile boolean system = false;

    @Resource
    private ApplicationContext applicationContext;

    @Override
    public void onApplicationEvent(ContextStartedEvent event) {
        system = true;
        LOGGER.info("push service instance {} inited success", GlobalConfig.getPushServiceId());
    }
}
