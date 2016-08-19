package com.be.android.library.worker.interfaces;

public interface FlagsProvider {
    boolean checkFlag(String flag);
    boolean hasFlag(String flag);
    void setFlag(String flag, boolean value);

    /**
     * synonym to setFlag(flag, true)
     * @param flag flag name
     */
    void setFlag(String flag);
    void removeFlag(String flag);
}
