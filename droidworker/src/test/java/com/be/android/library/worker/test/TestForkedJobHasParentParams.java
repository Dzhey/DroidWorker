package com.be.android.library.worker.test;

import com.be.android.library.worker.base.ForkJoinJob;
import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.base.JobStatus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.validateMockitoUsage;

/**
 * Verify that forked jobs' child can access parent job extras and flags
 *
 * Remember that SparseArray doesn't work with regular test runner so
 * ForkJoinJob.joinJob() and similar methods won't work
 */
@RunWith(PowerMockRunner.class)
public class TestForkedJobHasParentParams {

    private static final String TEST_FLAG = "test_flag";
    private static final String TEST_EXTRA = "test_extra";
    private static final String TEST_EXTRA_VALUE = "test_extra_value";

    private ForkJoinJob mParentJob;
    private ForkJoinJob mChildJob;
    private ForkJoinJob mParentExtraJob;
    private ForkJoinJob mChildExtraJob;

    @Before
    public void setUp() {
        mParentJob = new ForkJoinJob() {

            @Override
            protected void onPreExecute() throws Exception {
                mChildJob.setup().apply();
            }

            @Override
            protected JobEvent executeImpl() throws Exception {
                final ForkJoiner joiner = forkJob(mChildJob);
                final JobEvent result = joiner.get();

                if (result.getJobStatus() == JobStatus.OK) {
                    return JobEvent.ok();
                }

                return JobEvent.failure();
            }
        };

        mChildJob = new ForkJoinJob() {
            @Override
            protected JobEvent executeImpl() throws Exception {
                if (hasFlag(TEST_FLAG) && checkFlag(TEST_FLAG)) {
                    return JobEvent.ok();
                }

                return JobEvent.failure();
            }
        };

        mParentExtraJob = new ForkJoinJob() {

            @Override
            protected void onPreExecute() throws Exception {
                mChildExtraJob.setup().apply();
            }

            @Override
            protected JobEvent executeImpl() throws Exception {
                final ForkJoiner joiner = forkJob(mChildExtraJob);
                final JobEvent result = joiner.get();

                if (result.getJobStatus() == JobStatus.OK) {
                    return JobEvent.ok();
                }

                return JobEvent.failure();
            }
        };

        mChildExtraJob = new ForkJoinJob() {
            @Override
            protected JobEvent executeImpl() throws Exception {
                if (hasExtra(TEST_EXTRA) && TEST_EXTRA_VALUE.equals(findExtra(TEST_EXTRA))) {
                    return JobEvent.ok();
                }

                return JobEvent.failure();
            }
        };
    }

    @Test
    public void testCheckPositiveFlagValue() throws Exception {
        mParentJob.setup()
                .flag(TEST_FLAG)
                .apply();
        mParentJob.getParams().assignJobId(0);

        final JobEvent result = mParentJob.execute();

        assertEquals(JobEvent.EVENT_CODE_OK, result.getEventCode());
    }

    @Test
    public void testCheckNegativeFlagValue() throws Exception {
        mParentJob.setup()
                .flag(TEST_FLAG, false)
                .apply();
        mParentJob.getParams().assignJobId(0);

        final JobEvent result = mParentJob.execute();

        assertEquals(JobEvent.EVENT_CODE_FAILED, result.getEventCode());
    }

    @Test
    public void testHasParentExtra() throws Exception {
        mParentExtraJob.setup()
                .addExtra(TEST_EXTRA, TEST_EXTRA_VALUE)
                .apply();
        mParentExtraJob.getParams().assignJobId(0);

        final JobEvent result = mParentExtraJob.execute();

        assertEquals(JobEvent.EVENT_CODE_OK, result.getEventCode());
    }

    @Test
    public void testHasNotParentExtra() throws Exception {
        mParentExtraJob.setup().apply();
        mParentExtraJob.getParams().assignJobId(0);

        final JobEvent result = mParentExtraJob.execute();

        assertEquals(JobEvent.EVENT_CODE_FAILED, result.getEventCode());
    }

    @Test
    public void testHasInvalidParentExtra() throws Exception {
        mParentExtraJob.setup()
                .addExtra(TEST_EXTRA, "123")
                .apply();
        mParentExtraJob.getParams().assignJobId(0);

        final JobEvent result = mParentExtraJob.execute();

        assertEquals(JobEvent.EVENT_CODE_FAILED, result.getEventCode());
    }

    @After
    public void validate() {
        validateMockitoUsage();
    }
}
