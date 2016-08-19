package com.be.android.library.worker.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Flag implements Parcelable {

    public static final Creator<Flag> CREATOR = new Creator<Flag>() {
        @Override
        public Flag createFromParcel(Parcel parcel) {
            return new Flag(parcel);
        }

        @Override
        public Flag[] newArray(int sz) {
            return new Flag[sz];
        }
    };

    private String mName;
    private boolean mValue;

    public static Flag create(String flag, boolean value) {
        return new Flag(flag, value);
    }

    protected Flag(String flag, boolean value) {
        if (flag == null) {
            throw new IllegalArgumentException("null flag");
        }

        mName = flag;
        mValue = value;
    }

    protected Flag(Parcel source) {
        mName = source.readString();
        mValue = source.readByte() == 1;
    }

    public String getName() {
        return mName;
    }

    public boolean getValue() {
        return mValue;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mName);
        parcel.writeByte((byte) (mValue ? 1 : 0));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Flag flag = (Flag) o;

        if (mValue != flag.mValue) return false;
        if (!mName.equals(flag.mName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = mName.hashCode();
        result = 31 * result + (mValue ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Flag{" +
                "mName='" + mName + '\'' +
                ", mValue=" + mValue +
                '}';
    }
}
