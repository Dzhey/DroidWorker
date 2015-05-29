package com.be.android.library.worker.demo.ui.base;

import android.content.Intent;
import android.support.v7.widget.Toolbar;

public interface FragmentContainerCallbacks {
    void finish();
    void setResult(int resultCode, Intent data);
}
