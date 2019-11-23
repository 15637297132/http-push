package com.p7.framework.http.push;

import com.p7.framework.http.push.config.GlobalConfig;
import com.p7.framework.http.push.manage.BatchPushManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 如果使用mq，xml配置mq时要指定懒加载，服务启动后再这里的事件监听中
 * applicationContext.getBean获取一下mq的bean，使其生效
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

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"classpath:/spring-application.xml"});
        context.start();
        synchronized (SystemStartedEvent.class) {
            while (true) {
                try {
                    SystemStartedEvent.class.wait();
                } catch (InterruptedException e) {
                    LOGGER.error("== synchronized error:", e);
                }
            }
        }

    }
}
