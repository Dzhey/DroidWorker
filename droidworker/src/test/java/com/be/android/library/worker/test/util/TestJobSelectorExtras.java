package com.be.android.library.worker.test.util;

import com.be.android.library.worker.base.BaseJob;
import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.util.JobSelector;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.validateMockitoUsage;

public class TestJobSelectorExtras {

    private BaseJob mBaseJob;

    @Before
    public void setUp() {
        mBaseJob = new BaseJob() {
            @Override
            protected JobEvent executeImpl() throws Exception {
                return JobEvent.ok();
            }
        };

        mBaseJob.setup()
                .addExtra("extra.a", true)
                .addExtra("extra.b", false)
                .addExtra("extra.c", true)
                .apply();
    }

    @Test
    public void testAllExtrasMatched() throws Exception {
        JobSelector selector = new JobSelector()
                .addExtra("extra.a", true)
                .addExtra("extra.b", false)
                .addExtra("extra.c", true);

        assertTrue(selector.apply(mBaseJob));
    }

    @Test
    public void testPartExtrasMatched() throws Exception {
        JobSelector selector = new JobSelector()
                .addExtra("extra.a", true)
                .addExtra("extra.b", false);

        assertTrue(selector.apply(mBaseJob));
    }

    @Test
    public void testExtrasNotMatched() throws Exception {
        JobSelector selector = new JobSelector()
                .addExtra("extra.a", false)
                .addExtra("extra.b", true)
                .addExtra("extra.c", true);

        assertTrue(!selector.apply(mBaseJob));
    }

    @After
    public void validate() {
        validateMockitoUsage();
    }
}
