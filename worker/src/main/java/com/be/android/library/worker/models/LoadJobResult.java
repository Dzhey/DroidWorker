package com.be.android.library.worker.models;

import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.models.JobResultStatus;
import com.be.android.library.worker.base.JobStatus;

public class LoadJobResult<TData> extends JobEvent {
    private TData mData;

    public static <T> LoadJobResult<T> loadFailure() {
        return new LoadJobResult<T>(JobResultStatus.FAILED);
    }

    public static <T> LoadJobResult<T> loadOk() {
        return new LoadJobResult<T>(JobResultStatus.OK);
    }

    public static <TData> LoadJobResult fromEvent(JobEvent other, TData mData) {
        return new LoadJobResult<TData>(other, mData);
    }

    protected LoadJobResult(JobEvent other, TData mData) {
        super(other);
        this.mData = mData;
    }

    public LoadJobResult(LoadJobResult<TData> other) {
        super(other);

        mData = other.getData();
    }

    public LoadJobResult(int resultCode, JobStatus status, TData resultData) {
        setEventCode(resultCode);
        setJobStatus(status);

        mData = resultData;
    }

    public LoadJobResult(JobResultStatus status, TData resultData) {
        setJobStatus(status);
        setEventCode(JobResultStatus.getResultCode(status));

        mData = resultData;
    }

    public LoadJobResult(int resultCode, JobResultStatus status, TData resultData) {
        setEventCode(resultCode);
        setJobStatus(status);

        mData = resultData;
    }

    public LoadJobResult(JobResultStatus status) {
        setJobStatus(status);
        setEventCode(JobResultStatus.getResultCode(status));
    }

    public LoadJobResult(TData resultData) {
        setJobStatus(JobResultStatus.OK);
        setEventCode(JobEvent.EVENT_CODE_OK);

        mData = resultData;
    }

    public LoadJobResult() {
    }

    public LoadJobResult<TData> setExtraMessage(String message) {
        super.setExtraMessage(message);

        return this;
    }

    public void setEventCode(int eventCode) {
        super.setEventCode(eventCode);
    }

    public void setExtraCode(int extraCode) {
        super.setExtraCode(extraCode);
    }

    public TData getData() {
        return mData;
    }

    public void setData(TData data) {
        mData = data;
    }
}
