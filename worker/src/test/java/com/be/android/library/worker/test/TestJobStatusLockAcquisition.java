package com.be.android.library.worker.test;

import com.be.android.library.worker.base.BaseJob;
import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.base.JobStatusHolder;
import com.be.android.library.worker.base.JobStatusLock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.validateMockitoUsage;
import static org.mockito.Mockito.verify;

public class TestJobStatusLockAcquisition {

    private JobStatusLock mJobStatusLock;
    private BaseJob mBaseJob;

    @Before
    public void setUp() throws Exception {
        mJobStatusLock = mock(JobStatusLock.class);
        mBaseJob = new BaseJob() {
            @Override
            protected JobEvent executeImpl() throws Exception {
                return JobEvent.ok();
            }

            @Override
            protected JobStatusHolder createStatusHolder() {
                return new JobStatusHolder() {
                    @Override
                    protected JobStatusLock createLock() {
                        return mJobStatusLock;
                    }
                };
            }
        };
        mBaseJob.setup().apply();
        mBaseJob.getParams().assignJobId(0);
    }

    @Test
    public void testStatusLockAcquired() throws Exception {
        final JobStatusLock lock = mBaseJob.acquireStatusLock();
        try {
            mBaseJob.execute();
        } finally {
            lock.release();
        }

        verify(mJobStatusLock, times(1)).lock();
        verify(mJobStatusLock, times(1)).release();
    }

    @After
    public void validate() {
        validateMockitoUsage();
    }
}
