package com.be.android.library.worker.models;

import com.be.android.library.worker.controllers.JobManager;
import com.be.android.library.worker.interfaces.ParamsBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Params implements JobParams {

    public static class Builder implements ParamsBuilder {
        private final Params mParams;
        private boolean mIsBuilt;

        private Builder(Params params) {
            mParams = params;
        }

        @Override
        public Builder group(int groupId) {
            checkNotBuilt();

            mParams.mGroupId = groupId;

            return this;
        }

        @Override
        public Builder priority(int priority) {
            checkNotBuilt();

            mParams.mPriority = priority;

            return this;
        }

        @Override
        public Builder payload(Object payload) {
            checkNotBuilt();

            mParams.mPayload = payload;

            return this;
        }

        @Override
        public Builder tags(String... tags) {
            checkNotBuilt();

            if (tags.length == 0) {
                return this;
            }

            if (mParams.mJobTags == null) {
                mParams.mJobTags = new HashSet<String>(tags.length);
            } else {
                mParams.mJobTags.clear();
            }

            for (String tag : tags) {
                if (tag == null) {
                    throw new IllegalArgumentException("null tag provided");
                }

                mParams.mJobTags.add(tag);
            }

            return this;
        }

        @Override
        public Builder tags(Collection<String> tags) {
            checkNotBuilt();

            if (mParams.mJobTags == null) {
                mParams.mJobTags = new HashSet<String>(tags.size());
            } else {
                mParams.mJobTags.clear();
            }

            for (String tag : tags) {
                if (tag == null) {
                    throw new IllegalArgumentException("null tag provided");
                }

                mParams.mJobTags.add(tag);
            }

            return this;
        }

        @Override
        public Builder addTag(String tag) {
            checkNotBuilt();

            if (tag == null) {
                throw new IllegalArgumentException("tag == null");
            }

            if (mParams.mJobTags == null) {
                mParams.mJobTags = new HashSet<String>();
            }

            mParams.mJobTags.add(tag);

            return this;
        }

        @Override
        public Builder removeTag(String tag) {
            checkNotBuilt();

            if (tag == null) {
                throw new IllegalArgumentException("tag == null");
            }

            if (mParams.mJobTags != null) {
                mParams.mJobTags.remove(tag);
            }

            return this;
        }

        @Override
        public Builder addExtra(String key, Object value) {
            checkNotBuilt();

            if (mParams.mExtras == null) {
                mParams.mExtras = new HashMap<String, Object>();
            }

            mParams.mExtras.put(key, value);

            return this;
        }

        @Override
        public ParamsBuilder removeExtra(String key) {
            checkNotBuilt();

            if (mParams.mExtras == null) {
                return this;
            }

            mParams.mExtras.remove(key);

            return this;
        }

        @Override
        public ParamsBuilder flag(String flag, boolean value) {
            checkNotBuilt();

            mParams.setFlag(flag, value);

            return this;
        }

        public Params build() {
            checkNotBuilt();

            mIsBuilt = true;

            return mParams;
        }

        private void checkNotBuilt() {
            if (mIsBuilt) {
                throw new IllegalStateException("Params are already built");
            }
        }
    }

    private int mJobId = JobManager.JOB_ID_UNSPECIFIED;
    private int mGroupId;
    private int mPriority;
    private String mJobClassName;
    private Object mPayload;
    private Set<String> mJobTags;
    private Map<String, Object> mExtras;
    private Flags mFlags;

    protected Params() {
        mFlags = new Flags();
    }

    public static Builder create() {
        return new Builder(new Params());
    }

    @Override
    public Params copy() {
        Params p = new Params();
        p.mJobId = mJobId;
        p.mGroupId = mGroupId;
        p.mPriority = mPriority;
        p.mJobClassName = mJobClassName;
        p.mPayload = copyPayload();
        p.mJobTags = new HashSet<String>(mJobTags);
        p.mExtras = copyExtras();
        p.mFlags = mFlags.copy();

        return p;
    }

    public Builder copyWithBuilder() {
        return new Builder(copy());
    }

    @Override
    public boolean isJobIdAssigned() {
        return mJobId != JobManager.JOB_ID_UNSPECIFIED;
    }

    @Override
    public void assignJobId(int jobId) {
        if (mJobId != JobManager.JOB_ID_UNSPECIFIED) {
            throw new IllegalStateException("job id is already defined");
        }

        mJobId = jobId;
    }

    @Override
    public int getJobId() {
        return mJobId;
    }

    @Override
    public Flags getFlags() {
        return mFlags;
    }

    @Override
    public boolean checkFlag(String flag) {
        return mFlags.checkFlag(flag);
    }

    @Override
    public boolean hasFlag(String flag) {
        return mFlags.hasFlag(flag);
    }

    @Override
    public void setFlag(String flag, boolean value) {
        mFlags.setFlag(flag, value);
    }

    public void setJobClassName(String className) {
        if (mJobClassName != null) {
            throw new IllegalStateException("job class name is already defined");
        }

        mJobClassName = className;
    }

    @Override
    public String getJobClassName() {
        return mJobClassName;
    }

    @Override
    public int getGroupId() {
        return mGroupId;
    }

    @Override
    public int getPriority() {
        return mPriority;
    }

    @Override
    public boolean hasPayload() {
        return mPayload != null;
    }

    @Override
    public Object getPayload() {
        return mPayload;
    }

    @Override
    public Collection<String> getTags() {
        return Collections.unmodifiableCollection(mJobTags);
    }

    @Override
    public boolean hasTag(String tag) {
        return mJobTags != null && mJobTags.contains(tag);
    }

    @Override
    public boolean hasTags(String... tags) {
        if (mJobTags == null) {
            return false;
        }

        for (String tag : tags) {
            if (!mJobTags.contains(tag)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean hasTags(Collection<String> tags) {
        if (mJobTags == null) {
            return false;
        }

        return mJobTags.containsAll(tags);
    }

    @Override
    public Map<String, Object> getExtras() {
        return Collections.unmodifiableMap(mExtras);
    }

    @Override
    public Object getExtra(String key) {
        if (mExtras == null) {
            return null;
        }

        return mExtras.get(key);
    }

    @Override
    public <T> T getExtra(String key, T defaultValue) {
        try {
            return (T) getExtra(key);
        } catch (ClassCastException e) {
            throw new RuntimeException("failed to cast extra param for key '" + key + "'", e);
        }
    }

    @Override
    public boolean hasExtra(String key) {
        return mExtras != null && mExtras.containsKey(key);
    }

    protected Object copyPayload() {
        return mPayload;
    }

    protected Map<String, Object> copyExtras() {
        return new HashMap<String, Object>(mExtras);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Params params = (Params) o;

        if (mGroupId != params.mGroupId) return false;
        if (mJobId != params.mJobId) return false;
        if (mPriority != params.mPriority) return false;
        if (mExtras != null ? !mExtras.equals(params.mExtras) : params.mExtras != null)
            return false;
        if (mJobClassName != null ? !mJobClassName.equals(params.mJobClassName) : params.mJobClassName != null)
            return false;
        if (mJobTags != null ? !mJobTags.equals(params.mJobTags) : params.mJobTags != null)
            return false;
        if (mPayload != null ? !mPayload.equals(params.mPayload) : params.mPayload != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = mJobId;
        result = 31 * result + mGroupId;
        result = 31 * result + mPriority;
        result = 31 * result + (mJobClassName != null ? mJobClassName.hashCode() : 0);
        result = 31 * result + (mPayload != null ? mPayload.hashCode() : 0);
        result = 31 * result + (mJobTags != null ? mJobTags.hashCode() : 0);
        result = 31 * result + (mExtras != null ? mExtras.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Params{" +
                "mJobId=" + mJobId +
                ", mGroupId=" + mGroupId +
                ", mPriority=" + mPriority +
                ", mJobClassName='" + mJobClassName + '\'' +
                ", mPayload=" + mPayload +
                ", mJobTags=" + mJobTags +
                ", mExtras=" + mExtras +
                '}';
    }
}
