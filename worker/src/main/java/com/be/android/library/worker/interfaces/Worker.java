package com.be.android.library.worker.interfaces;

public interface Worker {
    public void submitJob(Job job);
    public void stop();
    public boolean isStopped();
}
