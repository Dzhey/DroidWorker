package com.be.android.library.worker.test;

import com.be.android.library.worker.base.BaseJob;
import com.be.android.library.worker.base.InstrumentationJobConfigurator;
import com.be.android.library.worker.base.JobConfigurator;
import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.base.JobStatus;
import com.be.android.library.worker.exceptions.JobExecutionException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.validateMockitoUsage;
import static org.mockito.Mockito.verify;

/**
 * Verify job behavior with various onCheckPreconditions result
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(BaseJob.class)
public class TestJobPreconditionsCheck {

    private BaseJob.ExecutionHandler mHandler;

    @Before
    public void setUp() throws Exception {
        mHandler = mock(BaseJob.ExecutionHandler.class);
    }

    @Test
    public void testSuccessPreconditionsCheck() throws Exception {
        final JobEvent pendingResult = JobEvent.ok();
        final BaseJob job = new BaseJob() {
            @Override
            protected JobEvent executeImpl() throws Exception {
                return pendingResult;
            }

            @Override
            protected JobConfigurator createConfigurator() {
                final InstrumentationJobConfigurator configurator =
                        new InstrumentationJobConfigurator(this, super.createConfigurator());

                configurator.setInstrumentationHandler(mHandler);

                return configurator;
            }
        };

        job.setup().apply();
        job.getParams().assignJobId(0);

        final JobEvent result = job.execute();

        assertSame(pendingResult, result);

        verify(mHandler, times(1)).onCheckPreconditions();
        verify(mHandler, times(1)).onPostExecute(refEq(result));
        verify(mHandler, never()).onExceptionCaught(Matchers.<Exception>anyObject());
        verify(mHandler, times(1)).onJobFinished(refEq(pendingResult));
        assertEquals(result.getJobStatus(), JobStatus.OK);
    }

    @Test
    public void testFailurePreconditionsCheck() throws Exception {
        final JobEvent pendingResult = JobEvent.failure();
        final BaseJob job = new BaseJob() {

            @Override
            protected JobEvent onCheckPreconditions() {
                return pendingResult;
            }

            @Override
            protected JobEvent executeImpl() throws Exception {
                return JobEvent.ok();
            }

            @Override
            protected JobConfigurator createConfigurator() {
                final InstrumentationJobConfigurator configurator =
                        new InstrumentationJobConfigurator(this, super.createConfigurator());

                configurator.setInstrumentationHandler(mHandler);

                return configurator;
            }
        };

        job.setup().apply();
        job.getParams().assignJobId(0);

        final JobEvent result = job.execute();

        verify(mHandler, times(1)).onCheckPreconditions();
        verify(mHandler, times(1)).onExceptionCaught(isA(JobExecutionException.class));
        verify(mHandler, never()).onPostExecute(Matchers.<JobEvent>anyObject());
        verify(mHandler, times(1)).onJobFinished(refEq(pendingResult));
        assertEquals(result.getJobStatus(), JobStatus.FAILED);
    }

    @Test
    public void testPreconditionsEventAndResultAreSame() {
        final String testErrorMessage = "test preconditions failure";
        final BaseJob job = new BaseJob() {
            @Override
            protected JobEvent onCheckPreconditions() {
                return JobEvent.failure(testErrorMessage);
            }

            @Override
            protected JobEvent executeImpl() throws Exception {
                return JobEvent.ok();
            }

            @Override
            protected JobConfigurator createConfigurator() {
                final InstrumentationJobConfigurator configurator =
                        new InstrumentationJobConfigurator(this, super.createConfigurator());

                configurator.setInstrumentationHandler(mHandler);

                return configurator;
            }
        };

        job.setup().apply();
        job.getParams().assignJobId(0);

        final JobEvent result = job.execute();

        assertTrue("test preconditions failure".equals(result.getExtraMessage()));

        final ArgumentCaptor<Exception> arg = ArgumentCaptor.forClass(Exception.class);
        verify(mHandler, times(1)).onExceptionCaught(arg.capture());
        final JobExecutionException ex = (JobExecutionException) arg.getValue();
        assertSame(result, ex.getJobEvent());
    }

    @After
    public void validate() {
        validateMockitoUsage();
    }
}
