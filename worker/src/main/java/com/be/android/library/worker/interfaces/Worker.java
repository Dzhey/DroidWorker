package com.be.android.library.worker.interfaces;

/**
 * Background job executor
 */
public interface Worker {

    /**
     * Submit job to be executed in background
     * @param job job to execute
     */
    public void submitJob(Job job);

    /**
     * Finish job execution and release allocated resources
     */
    public void finish();
}
