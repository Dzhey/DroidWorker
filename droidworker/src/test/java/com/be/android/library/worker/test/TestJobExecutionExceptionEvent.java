package com.be.android.library.worker.test;

import com.be.android.library.worker.base.BaseJob;
import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.base.JobStatus;
import com.be.android.library.worker.exceptions.JobExecutionException;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.validateMockitoUsage;

public class TestJobExecutionExceptionEvent {

    @Test
    public void testFailureLifecycle() throws Exception {
        final JobEvent expectedEvent = JobEvent.failure(
                "We're able to pass FAIL event via exception from onPreExecute");
        final Exception expectedException  = new JobExecutionException(expectedEvent);
        final BaseJob job = new BaseJob() {
            @Override
            protected void onPreExecute() throws Exception {
                throw expectedException;
            }

            @Override
            protected JobEvent executeImpl() throws Exception {
                return JobEvent.ok();
            }

            @Override
            protected void onExceptionCaught(Exception e) {
                Assert.assertSame(expectedException, e);

                JobEvent failureEvent = ((JobExecutionException)expectedException).getJobEvent();
                Assert.assertSame(expectedEvent, failureEvent);
                Assert.assertSame(expectedEvent.getExtraMessage(), failureEvent.getExtraMessage());
            }
        };
        job.setup().apply();
        job.getParams().assignJobId(0);

        final JobEvent result = job.execute();
        Assert.assertSame(expectedEvent, result);
    }

    @Test
    public void testInvalidFailureLifecycle() throws Exception {
        final JobEvent expectedEvent = JobEvent.ok(
                "We're unable to pass OK event via exception from onPreExecute");
        final Exception expectedException  = new JobExecutionException(expectedEvent);

        final BaseJob job = new BaseJob() {
            @Override
            protected void onPreExecute() throws Exception {
                throw expectedException;
            }

            @Override
            protected JobEvent executeImpl() throws Exception {
                return JobEvent.ok();
            }

            @Override
            protected void onExceptionCaught(Exception e) {
                Assert.assertTrue(e instanceof JobExecutionException);
                Assert.assertTrue(expectedException.getMessage().contains("illegal job result"));
            }
        };
        job.setup().apply();
        job.getParams().assignJobId(0);

        final JobEvent result = job.execute();
        assertEquals(JobStatus.FAILED, result.getJobStatus());
        assertEquals(JobEvent.EVENT_CODE_FAILED, result.getEventCode());
    }

    @After
    public void validate() {
        validateMockitoUsage();
    }
}
