package com.be.android.library.worker.test;

import com.be.android.library.worker.base.BaseJob;
import com.be.android.library.worker.base.JobEvent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.validateMockitoUsage;

public class TestJobRunAndCall {

    private BaseJob mBaseJob;
    private BaseJob.ExecutionHandler mExecutionHandler;

    @Before
    public void setUp() {
        mExecutionHandler = mock(BaseJob.ExecutionHandler.class);

        mBaseJob = spy(new BaseJob() {
            {
                setExecutionHandler(mExecutionHandler);
            }

            @Override
            protected JobEvent executeImpl() throws Exception {
                return JobEvent.ok();
            }
        });
    }

    @Test
    public void testRun() {
        mBaseJob.run();
        InOrder inOrder = inOrder(mBaseJob, mExecutionHandler);
        inOrder.verify(mBaseJob).run();
        inOrder.verify(mExecutionHandler).execute();
    }

    @Test
    public void testCall() {
        mBaseJob.call();
        InOrder inOrder = inOrder(mBaseJob, mExecutionHandler);
        inOrder.verify(mBaseJob).call();
        inOrder.verify(mExecutionHandler).execute();
    }

    @After
    public void validate() {
        validateMockitoUsage();
    }
}
