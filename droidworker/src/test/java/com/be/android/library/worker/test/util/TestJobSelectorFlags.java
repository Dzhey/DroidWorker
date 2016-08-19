package com.be.android.library.worker.test.util;

import com.be.android.library.worker.base.BaseJob;
import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.base.JobStatus;
import com.be.android.library.worker.util.JobSelector;

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

public class TestJobSelectorFlags {

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
                .flag("flag.a", true)
                .flag("flag.b", false)
                .flag("flag.c", true)
                .apply();
    }

    @Test
    public void testAllFlagsMatched() throws Exception {
        JobSelector selector = new JobSelector()
                .addFlag("flag.a", true)
                .addFlag("flag.b", false)
                .addFlag("flag.c", true);

        assertTrue(selector.apply(mBaseJob));
    }

    @Test
    public void testPartFlagsMatched() throws Exception {
        JobSelector selector = new JobSelector()
                .addFlag("flag.a", true)
                .addFlag("flag.b", false);

        assertTrue(selector.apply(mBaseJob));
    }

    @Test
    public void testFlagsNotMatched() throws Exception {
        JobSelector selector = new JobSelector()
                .addFlag("flag.a", false)
                .addFlag("flag.b", true)
                .addFlag("flag.c", true);

        assertTrue(!selector.apply(mBaseJob));
    }

    @Test
    public void testAllFlagsMatchedWithWildcardFlags() throws Exception {
        JobSelector selector = new JobSelector().flags("flag.a", "flag.b", "flag.c");

        assertTrue(selector.apply(mBaseJob));
    }

    @Test
    public void testFlagsNotMatchedWithWildcardFlags() throws Exception {
        JobSelector selector = new JobSelector().flags("flag.a", "flag.c", "flag.d");

        assertTrue(!selector.apply(mBaseJob));
    }

    @Test
    public void testAllFlagsMatchedWithAnyWildcardFlags() throws Exception {
        JobSelector selector = new JobSelector()
                .flags("flag.a", "flag.b", "flag.c", "flag.d")
                .setIsAnyFlag(true);

        assertTrue(selector.apply(mBaseJob));
    }

    @Test
    public void testFlagsMatchedWithAnyWildcardFlags() throws Exception {
        JobSelector selector = new JobSelector().flags("flag.b").setIsAnyFlag(true);

        assertTrue(selector.apply(mBaseJob));
    }

    @Test
    public void testFlagsNotMatchedWithAnyWildcardFlags() throws Exception {
        JobSelector selector = new JobSelector().flags("flag.d").setIsAnyFlag(true);

        assertTrue(!selector.apply(mBaseJob));
    }

    @Test
    public void testEmptyFlagsMatched() throws Exception {
        JobSelector selector = new JobSelector().flags();

        assertTrue(selector.apply(mBaseJob));
    }

    @Test
    public void testEmptyAnyFlagsMatched() throws Exception {
        JobSelector selector = new JobSelector().flags().setIsAnyFlag(true);

        assertTrue(selector.apply(mBaseJob));
    }

    @After
    public void validate() {
        validateMockitoUsage();
    }
}
