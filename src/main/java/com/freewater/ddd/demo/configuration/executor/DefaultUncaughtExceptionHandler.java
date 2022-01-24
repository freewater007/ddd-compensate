package com.freewater.ddd.demo.configuration.executor;

/**
 * 默认实现
 *
 * @auther FreeWater
 * @date 2021-2-4
 */
public class DefaultUncaughtExceptionHandler implements UncaughtExceptionHandler {
    @Override
    public void uncaughtException(Throwable e) {
        System.out.println("threadPool uncaughtException"+ e.toString());
    }
}