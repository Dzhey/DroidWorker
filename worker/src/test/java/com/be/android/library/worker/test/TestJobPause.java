package com.be.android.library.worker.test;

import com.be.android.library.worker.base.BaseJob;
import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.models.Params;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.validateMockitoUsage;

/**
 * Verify job pause/unpausecases
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(BaseJob.class)
public class TestJobPause {

    private BaseJob mBaseJob;

    @Before
    public void setUp() {
        mBaseJob = PowerMockito.spy(new BaseJob() {
            @Override
            protected JobEvent executeImpl() throws Exception {
                return JobEvent.ok();
            }

            @Override
            protected void onReset() {
                // empty overload to allow job reset
                // release held resources here
            }
        });
        mBaseJob.setup().apply();
        mBaseJob.getParams().assignJobId(0);
    }

    @Test
    public void testInitialPauseCount() {
        assertEquals(0, mBaseJob.getPauseCount());
    }

    @Test
    public void testSinglePauseCount() throws Exception {
        int count = mBaseJob.pause();

        assertEquals(1, count);
        assertEquals(count, mBaseJob.getPauseCount());

        count = mBaseJob.unpause();

        assertEquals(0, count);
        assertEquals(count, mBaseJob.getPauseCount());
    }

    @Test
    public void testDoublePauseCount() throws Exception {
        mBaseJob.pause();

        int count = mBaseJob.pause();

        assertEquals(2, count);

        mBaseJob.unpause();

        count = mBaseJob.unpause();

        assertEquals(0, count);
    }

    @Test
    public void testUnpauseAllResetsDoublePauseCount() throws Exception {
        mBaseJob.pause();
        mBaseJob.pause();

        mBaseJob.unpauseAll();

        assertEquals(0, mBaseJob.getPauseCount());
    }

    @Test
    public void testPauseCountForUnpauseAll() throws Exception {
        int count = mBaseJob.pause();

        assertEquals(1, count);

        mBaseJob.unpauseAll();

        count = mBaseJob.getPauseCount();

        assertEquals(0, count);
    }

    @Test
    public void testPauseAndUnpauseStatus() throws Exception {
        mBaseJob.pause();

        assertTrue(mBaseJob.getParams().checkFlag(Params.FLAG_JOB_PAUSED));

        mBaseJob.unpause();

        assertFalse(mBaseJob.getParams().checkFlag(Params.FLAG_JOB_PAUSED));
    }

    @Test
    public void testPauseAndUnpauseAllStatus() throws Exception {
        mBaseJob.pause();

        assertTrue(mBaseJob.getParams().checkFlag(Params.FLAG_JOB_PAUSED));

        mBaseJob.unpauseAll();

        assertFalse(mBaseJob.getParams().checkFlag(Params.FLAG_JOB_PAUSED));
    }

    @Test
    public void testDryUnpause() {
        mBaseJob.unpause();
        mBaseJob.unpauseAll();
        assertEquals(0, mBaseJob.getPauseCount());
    }

    @Test
    public void testSinglePauseAfterUnpauseAll() {
        mBaseJob.pause();
        mBaseJob.unpauseAll();
        assertEquals(0, mBaseJob.getPauseCount());
        assertEquals(1, mBaseJob.pause());
        assertTrue(mBaseJob.getParams().checkFlag(Params.FLAG_JOB_PAUSED));
    }

    @Test
    public void testResetPause() {
        mBaseJob.pause();
        mBaseJob.reset();
        assertEquals(0, mBaseJob.getPauseCount());
    }

    @Test
    public void testOnPerformPauseCalls() throws Exception {
        final BaseJob job = new BaseJob() {

            int mOnPerformPauseCount;

            @Override
            protected void onPerformPause() throws InterruptedException {
                // super call will perform actual pause using latch
                mOnPerformPauseCount++;
            }

            @Override
            protected void onPreExecute() throws Exception {
                super.onPreExecute();

                assertEquals(1, mOnPerformPauseCount);
            }

            @Override
            protected JobEvent executeImpl() throws Exception {
                assertEquals(2, mOnPerformPauseCount);

                return JobEvent.ok();
            }

            @Override
            protected void onPostExecute(JobEvent executionResult) throws Exception {
                super.onPostExecute(executionResult);

                assertEquals(3, mOnPerformPauseCount);
            }
        };

        job.setup().apply();
        job.getParams().assignJobId(0);
        job.pause();
        job.execute();
    }

    @After
    public void validate() {
        validateMockitoUsage();
    }
}
