package com.be.android.library.worker.test;

import com.be.android.library.worker.base.BaseJob;
import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.base.JobStatus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.validateMockitoUsage;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyPrivate;

/**
 * Verify job reset cases
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(BaseJob.class)
public class TestJobReset {

    @Before
    public void setUp() {
//        mBaseJob = PowerMockito.spy(new BaseJob() {
//            @Override
//            protected JobEvent executeImpl() throws Exception {
//                return JobEvent.ok();
//            }
//
//            @Override
//            protected void onReset() {
//            }
//        });
//        mBaseJob.setup().apply();
//        mBaseJob.getParams().assignJobId(0);
    }

    @Test
    public void testOnResetCalledOnce() throws Exception {
        final BaseJob job = PowerMockito.spy(new BaseJob() {
            @Override
            protected JobEvent executeImpl() throws Exception {
                return JobEvent.ok();
            }

            @Override
            protected void onReset() {
                // clean up your job state here
            }
        });
        job.reset();
        verifyPrivate(job, times(1)).invoke("onReset");
    }

    /**
     * If you forgot to override onReset(),
     * then exception should be thrown if you attempt to reset job
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testOnResetThrowWhenNotOverriden() {
        final BaseJob job = new BaseJob() {
            @Override
            protected JobEvent executeImpl() throws Exception {
                return JobEvent.ok();
            }
        };
        job.reset();
    }

    @Test
    public void testResetJobParams() {
        final BaseJob job = new BaseJob() {
            @Override
            protected JobEvent executeImpl() throws Exception {
                return JobEvent.ok();
            }

            @Override
            protected void onReset() {
            }
        };

        assertFalse(job.hasParams());

        job.setup().apply();

        assertTrue(job.hasParams());

        job.reset();

        assertFalse(job.hasParams());
    }

    @Test(expected = IllegalStateException.class)
    public void testResetInWrongState() {
        final BaseJob job = spy(new BaseJob() {
            @Override
            protected JobEvent executeImpl() throws Exception {
                return JobEvent.ok();
            }

            @Override
            protected void onReset() {
            }
        });

        when(job.getStatus()).thenReturn(JobStatus.IN_PROGRESS);

        job.reset();
    }

    @After
    public void validate() {
        validateMockitoUsage();
    }
}
