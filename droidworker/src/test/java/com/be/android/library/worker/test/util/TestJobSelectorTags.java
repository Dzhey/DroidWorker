package com.be.android.library.worker.test.util;

import com.be.android.library.worker.base.BaseJob;
import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.util.JobSelector;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.validateMockitoUsage;

public class TestJobSelectorTags {

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
                .tags("tag.a", "tag.b", "tag.c")
                .apply();
    }

    @Test
    public void testAllTagsMatched() throws Exception {
        JobSelector selector = new JobSelector()
                .tags("tag.a", "tag.b", "tag.c");

        assertTrue(selector.apply(mBaseJob));
    }

    @Test
    public void testPartTagsMatched() throws Exception {
        JobSelector selector = new JobSelector()
                .tags("tag.a", "tag.c");

        assertTrue(selector.apply(mBaseJob));
    }

    @Test
    public void testTagsNotMatched() throws Exception {
        JobSelector selector = new JobSelector()
                .tags("tag.a", "tag.c", "tag.d");

        assertTrue(!selector.apply(mBaseJob));
    }

    @Test
    public void testAnyTagsMatched() throws Exception {
        JobSelector selector = new JobSelector()
                .tags("tag.a", "tag.b", "tag.c", "tag.d")
                .setIsAnyTag(true);

        assertTrue(selector.apply(mBaseJob));
    }

    @Test
    public void testAnyTagsNotMatched() throws Exception {
        JobSelector selector = new JobSelector()
                .tags("tag.d")
                .setIsAnyTag(true);

        assertTrue(!selector.apply(mBaseJob));
    }

    @Test
    public void testEmptyTagsMatched() throws Exception {
        JobSelector selector = new JobSelector().tags();

        assertTrue(selector.apply(mBaseJob));
    }

    @Test
    public void testEmptyAnyTagsMatched() throws Exception {
        JobSelector selector = new JobSelector()
                .tags()
                .setIsAnyTag(true);

        assertTrue(selector.apply(mBaseJob));
    }

    @After
    public void validate() {
        validateMockitoUsage();
    }
}
