package com.be.android.library.worker.base;

import com.be.android.library.worker.interfaces.ParamsBuilder;

public interface JobConfigurator extends ParamsBuilder {
    void apply();
}
