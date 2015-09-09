package com.be.android.library.worker.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.be.android.library.worker.demo.ui.ForkJoinLoadDemoFragment;
import com.be.android.library.worker.demo.ui.JobCancelDemoFragment;
import com.be.android.library.worker.demo.ui.PauseJobDemoFragment;
import com.be.android.library.worker.demo.ui.MultiloadWorkDemoFragment;
import com.be.android.library.worker.demo.ui.SimpleLoadDemoFragment;
import com.be.android.library.worker.demo.ui.base.BaseFragment;
import com.be.android.library.worker.demo.ui.base.FragmentContainerActivity;


public class MainActivity extends BaseActivity {

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
        Bundle fragmentArgs = new Bundle();
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
            case 3:
                fragmentClass = MultiloadWorkDemoFragment.class;
                break;
            case 4:
                fragmentClass = MultiloadWorkDemoFragment.class;
                fragmentArgs.putBoolean(MultiloadWorkDemoFragment.ARG_ASYNC, true);
                break;
            case 5:
                fragmentClass = ForkJoinLoadDemoFragment.class;
                break;

            default:
                return;
        }
        Intent launchIntent = FragmentContainerActivity.prepareLaunchIntent(
                this,
                fragmentClass.getName(),
                fragmentArgs);
        startActivity(launchIntent);
    }
}