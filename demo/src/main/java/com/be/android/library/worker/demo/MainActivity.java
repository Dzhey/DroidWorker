package com.be.android.library.worker.demo;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.be.android.library.worker.demo.jobs.SimpleLoadResultJob;
import com.be.android.library.worker.annotations.OnJobFailure;
import com.be.android.library.worker.annotations.OnJobSuccess;
import com.be.android.library.worker.interfaces.Job;
import com.be.android.library.worker.models.LoadJobResult;


public class MainActivity extends BaseActivity {

    private static final String TAG_LOADER_SIMPLE = "MainActivity_loader_simple";

    private TextView mResultTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mResultTextView = (TextView) findViewById(R.id.resultView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            requestLoad(TAG_LOADER_SIMPLE);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @OnJobSuccess(SimpleLoadResultJob.class)
    public void onLoadSuccess(LoadJobResult<String> result) {
        mResultTextView.setText(result.getData());
    }

    @OnJobFailure(SimpleLoadResultJob.class)
    public void onLoadFailure() {
        mResultTextView.setText("error");
    }

    @Override
    public Job onCreateJob(String tag) {
        switch (tag) {
            case TAG_LOADER_SIMPLE:
                return new SimpleLoadResultJob();

            default:
                throw new IllegalArgumentException("unexpected loader tag: " + tag);
        }
    }
}
