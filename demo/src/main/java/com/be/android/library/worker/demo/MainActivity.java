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

import com.be.android.library.worker.demo.ui.JobCancelDemoFragment;
import com.be.android.library.worker.demo.ui.PauseJobDemoFragment;
import com.be.android.library.worker.demo.ui.SimpleLoadDemoFragment;
import com.be.android.library.worker.demo.ui.base.BaseFragment;
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
                handleListItemClick(pos);
            }
        });
    }

    private void handleListItemClick(final int pos) {
        Class<? extends BaseFragment> fragmentClass;
        switch (pos) {
            case 0:
                fragmentClass = SimpleLoadDemoFragment.class;
                break;
            case 1:
                fragmentClass = JobCancelDemoFragment.class;
                break;
            case 2:
                fragmentClass = PauseJobDemoFragment.class;
                break;

            default:
                return;
        }
        Intent launchIntent = FragmentContainerActivity.prepareLaunchIntent(
                this,
                fragmentClass.getName(),
                null);
        startActivity(launchIntent);
    }
}
