package com.be.android.library.worker.test;

import com.be.android.library.worker.base.BaseJob;
import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.base.JobStatus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.validateMockitoUsage;
import static org.mockito.Mockito.verify;

@RunWith(PowerMockRunner.class)
@PrepareForTest(BaseJob.class)
public class TestJobExecuteWithoutParams {

    private BaseJob mBaseJob;

    @Before
    public void setUp() {
        mBaseJob = PowerMockito.spy(new BaseJob() {

            @Override
            protected JobEvent executeImpl() throws Exception {
                return JobEvent.ok();
            }
        });

        doNothing().when(mBaseJob).notifyJobEvent(any(JobEvent.class));
    }

    @Test
    public void testThatSetupIsNotPerformed() {
        mBaseJob.execute();
        verify(mBaseJob, never()).setup();
    }

    @Test
    public void testThatNoParamsDefined() {
        try {
            mBaseJob.getParams();
        } catch (IllegalStateException e) {
            assertEquals("no params defined", e.getMessage());
        }
    }

    @Test
    public void testThatExecuteResultNotNull() {
        JobEvent result = mBaseJob.execute();
        assertNotNull(result);
    }

    @Test
    public void testThatExecuteResultIsFailure() {
        JobEvent result = mBaseJob.execute();

        assertEquals("job params are not defined", result.getExtraMessage());
        assertEquals(JobStatus.FAILED, result.getJobStatus());
        assertEquals(JobEvent.EVENT_CODE_FAILED, result.getEventCode());
        assertEquals(JobStatus.FAILED, mBaseJob.getStatus());
    }

    @Test
    public void testThatJobHasNoId() {
        assertEquals(false, mBaseJob.hasId());
        try {
            mBaseJob.getJobId();
        } catch (IllegalStateException e) {
            assertEquals("job id is not assigned", e.getMessage());
        }
    }

    @Test
    public void testStatusChangesFlow() {
        assertEquals(JobStatus.PENDING, mBaseJob.getStatus());

        mBaseJob.execute();

        // First time setStatus is called to notify that PROGRESS is started
        // Second time status silently set to failure
        try {
            PowerMockito.verifyPrivate(mBaseJob).invoke("setStatus", JobStatus.IN_PROGRESS);
            PowerMockito.verifyPrivate(mBaseJob).invoke("setStatusSilent", JobStatus.FAILED);
        } catch (Exception e) {
            assertTrue(false);
        }

        // In the end status is sent via notifyJobEvent with failure result
        ArgumentCaptor<JobEvent> arg = ArgumentCaptor.forClass(JobEvent.class);
        verify(mBaseJob, atLeastOnce()).notifyJobEvent(arg.capture());
        assertEquals(JobStatus.FAILED, arg.getValue().getJobStatus());
    }

    @Test
    public void testNotifyJobEventCallsOrderAndStatus() {
        mBaseJob.execute();

        // First time notify called with status updated to progress
        // Second time notify called with status updated to failure
        InOrder inOrder = inOrder(mBaseJob);
        ArgumentCaptor<JobEvent> arg = ArgumentCaptor.forClass(JobEvent.class);
        inOrder.verify(mBaseJob).notifyJobEvent(arg.capture());
        assertEquals(JobStatus.IN_PROGRESS, arg.getValue().getJobStatus());

        inOrder.verify(mBaseJob).notifyJobEvent(arg.capture());
        assertEquals(JobStatus.FAILED, arg.getValue().getJobStatus());
    }

    @After
    public void validate() {
        validateMockitoUsage();
    }
}
