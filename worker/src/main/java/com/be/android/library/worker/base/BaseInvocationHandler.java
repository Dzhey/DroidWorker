package com.be.android.library.worker.base;

import com.be.android.library.worker.interfaces.Job;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseInvocationHandler implements InvocationHandler {

    private final List<EventHandlerInvoker> mInvokers;
    private EventHandlerInvoker mApplicableInvoker;

    protected abstract Class<?> getPendingJobType();
    protected abstract JobStatus[] getPendingStatus();
    protected abstract int[] getPendingEventCode();
    protected abstract String[] getPendingTags();

    protected BaseInvocationHandler(Method method) {
        mInvokers = new ArrayList<EventHandlerInvoker>();
        mInvokers.add(new PolymorphicEventHandlerInvoker(method));
    }

    public void addEventHandlerInvoker(EventHandlerInvoker invoker) {
        mInvokers.add(0, invoker);
    }

    public boolean removeEventHandlerInvoker(EventHandlerInvoker invoker) {
        return mInvokers.remove(invoker);
    }

    protected boolean checkPendingJobType(JobEvent event) {
        Class<?> pendingJobType = getPendingJobType();

        if (pendingJobType == null || pendingJobType.equals(Job.class)) {
            return true;
        }

        return pendingJobType.getName().equals(event.getJobParams().getJobClassName());
    }

    protected boolean checkPendingStatus(JobEvent event) {
        final JobStatus[] pendingStatus = getPendingStatus();

        if (pendingStatus == null || pendingStatus.length == 0) {
            return true;
        }

        boolean isStatusMatched = false;
        for (JobStatus status : pendingStatus) {
            if (status == event.getJobStatus()) {
                isStatusMatched = true;
                break;
            }
        }

        return isStatusMatched;
    }

    protected boolean checkPendingEventCode(JobEvent event) {
        final int[] pendingEventCode = getPendingEventCode();

        if (pendingEventCode == null || pendingEventCode.length == 0) {
            return true;
        }

        boolean isEventCodeMatched = false;
        for (int eventCode : pendingEventCode) {
            if (eventCode == event.getEventCode()) {
                isEventCodeMatched = true;
                break;
            }
        }

        return isEventCodeMatched;
    }

    protected boolean checkPendingTags(JobEvent event) {
        final String[] pendingTags = getPendingTags();

        if (pendingTags == null || pendingTags.length == 0) {
            return true;
        }

        return event.getJobParams().hasTags(pendingTags);

    }

    @Override
    public boolean isFitEvent(Object receiver, JobEvent event) {
        if (!checkPendingStatus(event)
                || !checkPendingJobType(event)
                || !checkPendingEventCode(event)
                || !checkPendingTags(event)
                || mInvokers.isEmpty()) {

            return false;
        }

        for (EventHandlerInvoker invoker : mInvokers) {
            if (invoker.isFitEvent(event)) {
                mApplicableInvoker = invoker;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean canApply(Object receiver, JobEvent event) {
        if (!checkPendingStatus(event)
                || !checkPendingJobType(event)
                || !checkPendingEventCode(event)
                || !checkPendingTags(event)
                || mInvokers.isEmpty()) {

            return false;
        }

        for (EventHandlerInvoker invoker : mInvokers) {
            if (invoker.isApplicable(event)) {
                mApplicableInvoker = invoker;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean apply(Object receiver, JobEvent event) throws Exception {
        if (mApplicableInvoker != null
                && mApplicableInvoker.isApplicable(event)) {

            mApplicableInvoker.invoke(receiver, event);

            return true;
        }

        if (!canApply(receiver, event)) {
            return false;
        }

        mApplicableInvoker.invoke(receiver, event);

        return true;
    }
}
