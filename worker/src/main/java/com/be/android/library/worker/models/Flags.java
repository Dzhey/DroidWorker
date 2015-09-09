package com.be.android.library.worker.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.be.android.library.worker.interfaces.FlagsProvider;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class Flags implements Parcelable, FlagsProvider {

    public static interface OnFlagSetListener {
        void onFlagSet(Flags flags, String flag, boolean newValue, boolean hasChanged);
    }

    public static class Builder {
        private final Flags mFlags;
        private boolean mIsBuilt;

        public Builder() {
            mFlags = new Flags();
        }

        public Builder withFlag(String flag, boolean value) {
            checkNotBuilt();

            mFlags.setFlag(flag, value);

            return this;
        }

        public Flags build() {
            checkNotBuilt();

            mIsBuilt = true;

            return mFlags;
        }

        private void checkNotBuilt() {
            if (mIsBuilt) {
                throw new IllegalStateException("flags already built");
            }
        }
    }

    public static final Creator<Flags> CREATOR = new Creator<Flags>() {
        @Override
        public Flags createFromParcel(Parcel parcel) {
            return new Flags(parcel);
        }

        @Override
        public Flags[] newArray(int sz) {
            return new Flags[sz];
        }
    };

    private final Object mMutex;
    private Map<String, Boolean> mFlags;
    private List<WeakReference<OnFlagSetListener>> mOnFlagSetListeners;

    public Flags() {
        mMutex = new Object();
    }

    public Builder create() {
        return new Builder();
    }

    protected Flags(Parcel source) {
        this();

        boolean hasFlags = source.readByte() == 1;
        if (!hasFlags) {
            return;
        }

        final int sz = source.readInt();
        mFlags = new HashMap<String, Boolean>(sz);
        for (int i = 0; i < sz; i++) {
            String key = source.readString();
            Boolean value = source.readByte() == 1;
            mFlags.put(key, value);
        }
    }

    public Flags copy() {
        Flags copy = new Flags();

        if (mFlags != null) {
            copy.mFlags = new HashMap<String, Boolean>(mFlags);
        }

        return copy;
    }

    @Override
    public boolean checkFlag(String flag) {
        if (flag == null) {
            throw new IllegalArgumentException("null flag passed");
        }

        return mFlags != null && mFlags.containsKey(flag) && mFlags.get(flag);
    }

    @Override
    public void setFlag(String flag, boolean value) {
        if (flag == null) {
            throw new IllegalArgumentException("null flag passed");
        }

        if (mFlags == null) {
            mFlags = new HashMap<String, Boolean>();
        }

        final boolean hasChanged = !hasFlag(flag) || checkFlag(flag) != value;

        if (hasChanged) {
            mFlags.put(flag, value);
        }

        notifyFlagSet(flag, value, hasChanged);
    }

    @Override
    public void setFlag(String flag) {
        setFlag(flag, true);
    }

    @Override
    public void removeFlag(String flag) {
        if (flag == null) {
            throw new IllegalArgumentException("null flag passed");
        }

        if (mFlags == null) {
            return;
        }

        mFlags.remove(flag);
    }

    @Override
    public boolean hasFlag(String flag) {
        if (flag == null) {
            throw new IllegalArgumentException("null flag passed");
        }

        return mFlags != null && mFlags.containsKey(flag);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        if (mFlags == null) {
            parcel.writeByte((byte) 0);
            return;
        }

        parcel.writeByte((byte) 1);
        final int sz = mFlags.size();
        parcel.writeInt(sz);
        for (Map.Entry<String, Boolean> flag : mFlags.entrySet()) {
            parcel.writeString(flag.getKey());
            parcel.writeByte((byte) (flag.getValue() ? 1 : 0));
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public boolean removeOnFlagSetListener(OnFlagSetListener onFlagSetListener) {
        if (mOnFlagSetListeners == null) {
            return false;
        }

        synchronized (mMutex) {
            final Iterator<WeakReference<OnFlagSetListener>> iter = mOnFlagSetListeners.iterator();
            while (iter.hasNext()) {
                OnFlagSetListener listener = iter.next().get();

                if (listener == null) {
                    iter.remove();
                    continue;
                }

                if (onFlagSetListener.equals(listener)) {
                    iter.remove();
                    return true;
                }
            }

            return false;
        }
    }

    public void addOnFlagSetListener(OnFlagSetListener onFlagSetListener) {
        if (onFlagSetListener == null) {
            throw new IllegalArgumentException("onFlagSetListener may not be null");
        }

        synchronized (mMutex) {
            if (mOnFlagSetListeners == null) {
                mOnFlagSetListeners = new CopyOnWriteArrayList<WeakReference<OnFlagSetListener>>();
            }

            for (WeakReference<OnFlagSetListener> ref : mOnFlagSetListeners) {
                OnFlagSetListener listener = ref.get();

                if (listener != null && listener.equals(onFlagSetListener)) {
                    return;
                }
            }

            mOnFlagSetListeners.add(new WeakReference<OnFlagSetListener>(onFlagSetListener));
        }
    }

    protected void notifyFlagSet(String flag, boolean newValue, boolean hasChanged) {
        synchronized (mMutex) {
            if (mOnFlagSetListeners == null) {
                return;
            }

            for (WeakReference<OnFlagSetListener> ref : mOnFlagSetListeners) {
                OnFlagSetListener listener = ref.get();

                if (listener != null) {
                    listener.onFlagSet(this, flag, newValue, hasChanged);
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Flags flags = (Flags) o;

        if (mFlags != null ? !mFlags.equals(flags.mFlags) : flags.mFlags != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return mFlags != null ? mFlags.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Flags{" +
                "mFlags=" + mFlags +
                '}';
    }
}
