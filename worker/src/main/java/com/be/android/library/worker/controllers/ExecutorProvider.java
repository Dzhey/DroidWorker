package com.be.android.library.worker.controllers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

class ExecutorProvider {
    private ExecutorService executor;
    private ThreadFactory threadFactory;

    ExecutorProvider(ThreadFactory threadFactory) {

        this.threadFactory = threadFactory;
    }

    public ExecutorService get() {
        if (executor == null) {
            executor = Executors.newSingleThreadExecutor(threadFactory);
        }

        return executor;
    }
}
