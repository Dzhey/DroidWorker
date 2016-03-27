package com.be.android.library.worker.demo.test;

import android.support.test.espresso.IdlingResource;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;

import com.be.android.library.worker.annotations.OnJobSuccess;
import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.demo.BaseActivity;
import com.be.android.library.worker.demo.MainActivity;
import com.be.android.library.worker.demo.R;
import com.be.android.library.worker.handlers.JobEventDispatcher;
import com.be.android.library.worker.interfaces.Job;

import org.junit.Rule;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.Espresso.registerIdlingResources;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

    static class IdlingJob implements IdlingResource {

        private final int mJobId;
        private JobEventDispatcher mJobEventDispatcher;
        private ResourceCallback mResourceCallback;


        IdlingJob(int jobId, JobEventDispatcher dispatcher) {
            mJobId = jobId;
            mJobEventDispatcher = dispatcher;
            mJobEventDispatcher.register(this);
        }

        @OnJobSuccess
        public void onJobSuccess() {
            mResourceCallback.onTransitionToIdle();
            mJobEventDispatcher.unregister(this);
        }

        @Override
        public String getName() {
            return "job";
        }

        @Override
        public boolean isIdleNow() {
            return mJobEventDispatcher.isPending(mJobId);
        }

        @Override
        public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
            mResourceCallback = resourceCallback;
        }
    }

    private JobEventDispatcher mJobEventDispatcher;
    private int mPendingJobId;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mPendingJobId = 0;
        mJobEventDispatcher = new JobEventDispatcher(getActivity()) {
            @Override
            public boolean addPendingJob(int jobId) {
                mPendingJobId = jobId;

                return super.addPendingJob(jobId);
            }
        };
        getActivity().setJobEventDispatcher(mJobEventDispatcher);
    }

    public void testJobSuccess() {
        openActionBarOverflowOrOptionsMenu(getActivity());
        onView(withText(R.string.action_settings)).perform(click());

        registerIdlingResources(new IdlingJob(mPendingJobId, mJobEventDispatcher));
//        onView(withId(R.id.resultView)).check(matches(withText("success!")));
    }

}