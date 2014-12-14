package com.be.android.library.worker.base;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JobEventParcel implements Parcelable {

    public static final Creator<JobEventParcel> CREATOR =
            new Creator<JobEventParcel>() {
                @Override
                public JobEventParcel createFromParcel(Parcel parcel) {
                    return new JobEventParcel(parcel);
                }

                @Override
                public JobEventParcel[] newArray(int size) {
                    return new JobEventParcel[size];
                }
            };

    private JobEvent event;

    public JobEventParcel(JobEvent event) {
        this.event = event;
    }

    public JobEvent getJobEvent() {
        return event;
    }

    private JobEventParcel(Parcel source) {
        event = new JobEvent(0);
        event.setJobId(source.readInt());
        event.setJobFinished(source.readByte() == 1);
        event.setJobGroupId(source.readInt());
        event.setEventCode(source.readInt());
        event.setExtraMessage(source.readString());
        event.setJobStatus(JobStatus.valueOf(source.readString()));
        final int tagsCount = source.readInt();
        if (tagsCount == 0) {
            event.setJobTags(Collections.EMPTY_LIST);
        } else {
            final List<String> tags = new ArrayList<String>(tagsCount);
            for (int i = 0; i < tagsCount; i++) {
                tags.add(source.readString());
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(event.getJobId());
        parcel.writeByte((byte) (event.isJobFinished() ? 1 : 0));
        parcel.writeInt(event.getJobGroupId());
        parcel.writeInt(event.getEventCode());
        parcel.writeString(event.getExtraMessage());
        parcel.writeString(event.getJobStatus().name());
        List<String> tags = event.getJobTags();
        final int sz = tags.size();
        parcel.writeInt(sz);
        for (String tag : tags) {
            parcel.writeString(tag);
        }
    }
}
