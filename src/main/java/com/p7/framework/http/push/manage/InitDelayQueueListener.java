package com.p7.framework.http.push.manage;

import com.p7.framework.http.push.task.BatchPushTask;
import com.p7.framework.http.push.task.SinglePushTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.Resource;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Yangzhen
 **/
public class InitDelayQueueListener implements InitializingBean, DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitDelayQueueListener.class);

    @Resource
    private ThreadPoolTaskExecutor batchPushThreadPool;

    @Resource
    private ThreadPoolTaskExecutor singlePushThreadPool;

    @Override
    public void afterPropertiesSet() throws Exception {
        startThread();
    }

    @Override
    public void destroy() throws Exception {
        batchPushThreadPool.destroy();
        singlePushThreadPool.destroy();
    }

    private void startThread() {
        LOGGER.info("init single push delay queue ...");
        for (int i = 0; i < SinglePushControl.delayQueues.size(); i++) {
            final DelayQueue<SinglePushTask> singlePushTasks = SinglePushControl.get(i);
            singlePushThreadPool.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            TimeUnit.MILLISECONDS.sleep(100);
                            // 如果当前活动线程等于最大线程，那么不执行
                            if (singlePushThreadPool.getActiveCount() < singlePushThreadPool.getMaxPoolSize()) {
                                /**
                                 * 不能使用take，否则会一直阻塞
                                 */
                                final SinglePushTask task = singlePushTasks.poll();
                                if (task != null) {
                                    singlePushThreadPool.execute(task);
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.error("系统异常", e);
                        e.printStackTrace();
                    }
                }
            });
        }
        LOGGER.info("init batch push delay queue ...");
        for (int i = 0; i < BatchPushControl.taskQueues.size(); i++) {
            final DelayQueue<BatchPushTask> batchPushTasks = BatchPushControl.taskQueues.get(i);
            batchPushThreadPool.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            TimeUnit.MILLISECONDS.sleep(100);
                            // 如果当前活动线程等于最大线程，那么不执行
                            if (batchPushThreadPool.getActiveCount() < batchPushThreadPool.getMaxPoolSize()) {
                                /**
                                 * 不能使用take，否则会一直阻塞
                                 */
                                final BatchPushTask task = batchPushTasks.poll();
                                if (task != null) {
                                    batchPushThreadPool.execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            task.ifRun = true;
                                            task.run();
                                        }
                                    });
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.error("系统异常", e);
                        e.printStackTrace();
                    }
                }
            });
        }

    }
}
