package com.be.android.library.worker.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.be.android.library.worker.demo.ui.PauseJobDemoFragment;
import com.be.android.library.worker.demo.ui.base.FragmentContainerActivity;


public class MainActivity extends BaseActivity {

    private static final String TAG_LOADER_SIMPLE = "MainActivity_loader_simple";

    private ListView mListView;
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(R.string.app_name);

        mListView = (ListView) findViewById(android.R.id.list);
        mListView.setAdapter(ArrayAdapter.createFromResource(
                this,
                R.array.demo_list,
                android.R.layout.simple_list_item_1
        ));
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
                long itemId = mListView.getAdapter().getItemId(pos);
                handleListItemClick();
            }
        });
    }

    private void handleListItemClick() {
        Intent launchIntent = FragmentContainerActivity.prepareLaunchIntent(
                this,
                PauseJobDemoFragment.class.getName(),
                null);
        startActivity(launchIntent);
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

    /*@OnJobSuccess(SimpleLoadResultJob.class)
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
    }*/
}
