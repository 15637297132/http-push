package com.p7.framework.http.push.manage;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Yangzhen
 * @Description
 **/
public class ScanDatabaseFactory implements ThreadFactory {

    private AtomicInteger atomicInteger = new AtomicInteger(0);

    private String threadNamePrefix;

    @Override
    public Thread newThread(Runnable r) {
        int threadCounter = atomicInteger.incrementAndGet();
        return new Thread(r, threadNamePrefix + threadCounter);
    }

    public void setThreadNamePrefix(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
    }
}
