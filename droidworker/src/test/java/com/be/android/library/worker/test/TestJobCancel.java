package com.be.android.library.worker.test;

import com.be.android.library.worker.base.BaseJob;
import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.base.JobStatus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.validateMockitoUsage;
import static org.mockito.Mockito.verify;

/**
 * Verify job cancel cases
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(BaseJob.class)
public class TestJobCancel {

    private BaseJob mBaseJob;

    @Before
    public void setUp() {
        mBaseJob = PowerMockito.spy(new BaseJob() {
            @Override
            protected JobEvent executeImpl() throws Exception {
                return JobEvent.ok();
            }
        });
        mBaseJob.setup().apply();
        mBaseJob.getParams().assignJobId(0);
    }

    @Test
    public void testCancelResultBeforeExecute() throws Exception {
        mBaseJob.cancel();

        final JobEvent result = mBaseJob.execute();

        assertEquals(JobStatus.CANCELLED, mBaseJob.getStatus());
        assertEquals(JobEvent.EVENT_CODE_CANCELLED, result.getEventCode());
    }

    @Test
    public void testCancelStatus() {
        mBaseJob.cancel();

        assertTrue(mBaseJob.isCancelled());

        final JobEvent result = mBaseJob.execute();

        assertEquals(JobStatus.CANCELLED, mBaseJob.getStatus());
        assertEquals(JobEvent.EVENT_CODE_CANCELLED, result.getEventCode());
    }

    @Test
    public void testOnCancelledCalled() throws Exception {
        mBaseJob.cancel();

        PowerMockito.verifyPrivate(mBaseJob, times(1)).invoke("onCancelled");
    }

    @Test
    public void testOnCancelledCalledOnlyOnce() throws Exception {
        mBaseJob.cancel();
        mBaseJob.cancel();

        PowerMockito.verifyPrivate(mBaseJob, times(1)).invoke("onCancelled");
    }

    @Test
    public void testLastJobEventIsCancelEvent() {
        mBaseJob.cancel();
        mBaseJob.execute();

        final ArgumentCaptor<JobEvent> arg = ArgumentCaptor.forClass(JobEvent.class);

        verify(mBaseJob, atLeast(1)).notifyJobEvent(arg.capture());

        final JobEvent lastJobEvent = arg.getAllValues().get(arg.getAllValues().size() - 1);
        assertEquals(JobStatus.CANCELLED, lastJobEvent.getJobStatus());
        assertEquals(JobEvent.EVENT_CODE_CANCELLED, lastJobEvent.getEventCode());
    }

    @After
    public void validate() {
        validateMockitoUsage();
    }
}
