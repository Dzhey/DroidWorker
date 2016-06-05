# DroidWorker
Flexible background processing library for Android made with respect to the app lifecycle
## Features
 * __Serial__ or __Asynchronous__ kind of execution for your _Jobs_ (and more)
 * __Simple Job lifecycle__ to better structure your _Job_ code and handle errors
 * __Cancellation__ available at any time
 * __Pause/Resume__ feature
 * __Single entry point__ to access and manage submitted _Jobs_ using _JobSelectors_
 * __Prioritization__ to neatly influence execution order
 * __ForkJoinJob__ allowing to reuse and combine previously written _Jobs_ in different ways
 * __Activity/Fragment bindings__ with annotation-styled subscription: you determine when it's time to subscribe or remove subscription for your component
 * __Job parameteres__ injection powered by code generation and annotations
 * __Job reset__ feature combined with builder-styled initializer to reuse _Job_ instance

## Hello World
Most-common _Job_ use case is _Loader_ replacement, but you can use _Job_ for any purpose (networking, database operations, data processing, etc)

### UI code
```java

public class DemoUserViewFragment extends BaseFragment {

      // Tag to associate with the Job
      private static final String TAG_LOAD_JOB = "DemoUserViewFragment_User_Loader";

      ...

      @Override
      public void onResume() {
        super.onResume();

        // requestLoad() will start our Job only if there are
        // no any running or queued Job instance with specified tag
        requestLoad(TAG_LOAD_JOB);
      }

      @Override
      public Job onCreateJob(String attachTag) {
        // attachTag will be TAG_LOAD_JOB
        // you may switch between tags to distinct separate load
        // requests and return different Jobs when needed

        return new LoadUserJob()
                .setup()
                .configure(DemoUserViewFragment.captureExtras()
                        .setUserId(mUserId)
                        .apply())
                .getJob();
      }

      @OnJobSuccess(DemoUserViewFragment.class)
      public void onUserLoaded(LoadJobResult<User> result) {
        final User user = result.getData();
        // do everything you need to display user data
      }

      @OnJobFailure(DemoUserViewFragment.class)
      public void onUserLoadFailure(JobEvent event) {
        // display error (event argument is optional and it can contain detailed error info)
      }
}
```
Please note that some common code including event subscription is moved to the base class (see demo sample for details).

### Job code
```java
public class LoadUserJob extends LoadJob {

    @JobExtra
    long mUserId;

    ...

    @Override
    protected void onPreExecute() throws Exception {
        super.onPreExecute();

        // LoadUserJobJobExtras is generated from Job annotations
        // From this point mUserId will contain actual value
        // previously specified in onCreateJob() callback
        LoadUserJobJobExtras.injectExtras(this);

        if (mUserId == 0) {
            // It is safe to throw exception from any callback method
            // UI will receive regular failure event
            throw new IllegalArgumentException("user id is not specified");
        }
    }

    @Override
    protected LoadJobResult<User> performLoad() throws Exception {
        final Response<User> response = mRestClient.getUser(mUserId).execute();

        if (!response.isSuccessful()) {
            // return regular load failure event
            return LoadJobResult.loadFailure();
        }

        return new LoadJobResult<>(response.body());
      }
}
 ```
## Setup
_coming soon_