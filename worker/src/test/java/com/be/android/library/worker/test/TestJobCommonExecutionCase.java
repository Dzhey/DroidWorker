package com.be.android.library.worker.test;

import com.be.android.library.worker.base.BaseJob;
import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.base.JobStatus;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.validateMockitoUsage;

public class TestJobCommonExecutionCase {

    @Test
    public void testExecuteSuccess() {
        final BaseJob baseJob = new BaseJob() {
            @Override
            protected JobEvent executeImpl() throws Exception {
                return JobEvent.ok();
            }
        };

        baseJob.setup().apply();
        baseJob.getParams().assignJobId(1);

        final JobEvent result = baseJob.execute();

        assertEquals(JobStatus.OK, result.getJobStatus());
    }

    @Test
    public void testExecuteFailure() {
        final BaseJob baseJob = new BaseJob() {
            @Override
            protected JobEvent executeImpl() throws Exception {
                return JobEvent.failure();
            }
        };

        baseJob.setup().apply();

        baseJob.getParams().assignJobId(1);

        final JobEvent result = baseJob.execute();

        assertEquals(JobStatus.FAILED, result.getJobStatus());
    }

    @Test
    public void testExecuteThrowException() {
        final Exception ex = new Exception();
        final BaseJob baseJob = new BaseJob() {
            @Override
            protected JobEvent executeImpl() throws Exception {
                throw ex;
            }
        };

        baseJob.setup().apply();

        baseJob.getParams().assignJobId(1);

        final JobEvent result = baseJob.execute();

        assertEquals(JobStatus.FAILED, result.getJobStatus());
        assertSame(ex, result.getUncaughtException());
    }

    @After
    public void validate() {
        validateMockitoUsage();
    }
}
