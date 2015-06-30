package com.be.android.library.worker.demo;

import android.app.Application;

import com.be.android.library.worker.controllers.JobManager;
import com.be.android.library.worker.controllers.WorkerJobManager;

public class App extends Application {

    private static JobManager sJobManager;
    private static App sInstance;

    @Override
    public void onCreate() {
        super.onCreate();

        sInstance = this;

        // From default constructor ThreadPoolWorkerService will be used,
        // so don't forget to add <service> to manifest
        //
        // You can use own WorkerService implementation -
        // all you need is just create suitable worker to execute jobs
        sJobManager = new WorkerJobManager(this);

        JobManager.init(sJobManager);
    }

    /*
     * It's better to use dependency injection outside of the demo
     */
    public static JobManager getJobManager() {
        return sJobManager;
    }

    public static App getInstance() {
        return sInstance;
    }
}
