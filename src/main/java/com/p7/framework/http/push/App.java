package com.p7.framework.http.push;

import com.p7.framework.http.push.service.HttpPushService;
import com.p7.framework.http.push.test.ConcurrentBatchTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;


/**
 * 测试时，注释掉spring-push.xml中的<bean id="initDelayQueueListener" class="com.p7.framework.http.push.manage.InitDelayQueueListener"/>
 *
 * @author Yangzhen
 **/
public class App {
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    private static ClassPathXmlApplicationContext context;

    public static HttpPushService httpPushService;

    public static void main(String[] args) {
        try {
            context = new ClassPathXmlApplicationContext(new String[]{"classpath:/spring-application.xml"});
            context.start();
            httpPushService = (HttpPushService) context.getBean("httpPushService");
            batch();
            LOGGER.info("== context start");
        } catch (Exception e) {
            LOGGER.error("== application start error:", e);
            return;
        }
        synchronized (App.class) {
            while (true) {
                try {
                    App.class.wait();
                } catch (InterruptedException e) {
                    LOGGER.error("== synchronized error:", e);
                }
            }
        }
    }

    private static void batch() {
        ConcurrentBatchTask task = new ConcurrentBatchTask(httpPushService);
        task.process();
    }
}
