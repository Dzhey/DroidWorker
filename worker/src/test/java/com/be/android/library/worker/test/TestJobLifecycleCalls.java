package com.be.android.library.worker.test;

import com.be.android.library.worker.base.BaseJob;
import com.be.android.library.worker.base.JobEvent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.validateMockitoUsage;
import static org.mockito.Mockito.when;

/**
 * Verify job lifecycle callbacks order
 */
public class TestJobLifecycleCalls {

    private BaseJob mBaseJob;
    private BaseJob.ExecutionHandler mExecutionHandler;

    @Before
    public void setUp() throws Exception {
        mExecutionHandler = mock(BaseJob.ExecutionHandler.class);
        mBaseJob = new BaseJob() {
            {
                setExecutionHandler(mExecutionHandler);
            }

            @Override
            protected JobEvent executeImpl() throws Exception {
                return JobEvent.ok();
            }
        };
        mBaseJob.setup().apply();
        mBaseJob.getParams().assignJobId(0);
    }

    @Test
    public void testSuccessLifecycle() throws Exception {
        when(mExecutionHandler.executeImpl()).thenReturn(JobEvent.ok());

        mBaseJob.execute();
        InOrder inOrder = inOrder(mExecutionHandler);
        inOrder.verify(mExecutionHandler).onPreExecute();
        inOrder.verify(mExecutionHandler).executeImpl();
        inOrder.verify(mExecutionHandler).onPostExecute(any(JobEvent.class));
    }

    @Test
    public void testFailureLifecycle() throws Exception {
        Exception expectedException = new RuntimeException("test throw");
        when(mExecutionHandler.executeImpl()).thenThrow(expectedException);

        mBaseJob.execute();

        InOrder inOrder = inOrder(mExecutionHandler);
        inOrder.verify(mExecutionHandler).onPreExecute();
        inOrder.verify(mExecutionHandler).executeImpl();
        inOrder.verify(mExecutionHandler).onExceptionCaught(refEq(expectedException));
    }

    @After
    public void validate() {
        validateMockitoUsage();
    }
}
