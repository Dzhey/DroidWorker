package com.be.android.library.worker.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.be.android.library.worker.demo.ui.ForkJoinLoadDemoFragment;
import com.be.android.library.worker.demo.ui.GitHubDemoFragment;
import com.be.android.library.worker.demo.ui.JobCancelDemoFragment;
import com.be.android.library.worker.demo.ui.MultiloadWorkDemoFragment;
import com.be.android.library.worker.demo.ui.PauseJobDemoFragment;
import com.be.android.library.worker.demo.ui.SimpleLoadDemoFragment;
import com.be.android.library.worker.demo.ui.base.BaseFragment;
import com.be.android.library.worker.demo.ui.base.FragmentContainerActivity;


public class MainActivity extends BaseActivity {

    private ListView mListView;
    private Toolbar mToolbar;
    private String[] mTitles;
    private String[] mHints;

    private final BaseAdapter mListAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return mTitles.length;
        }

        @Override
        public String getItem(int position) {
            return mTitles[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this)
                        .inflate(android.R.layout.simple_list_item_2, parent, false);
            }

            ((TextView) convertView.findViewById(android.R.id.text1))
                    .setText(mTitles[position]);
            ((TextView) convertView.findViewById(android.R.id.text2))
                    .setText(mHints[position]);

            return convertView;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(R.string.app_name);

        mTitles = getResources().getStringArray(R.array.demo_list);
        mHints = getResources().getStringArray(R.array.demo_list_hints);
        mListView = (ListView) findViewById(android.R.id.list);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
                handleListItemClick(pos);
            }
        });
        mListView.setAdapter(mListAdapter);
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
            case 6:
                fragmentClass = GitHubDemoFragment.class;
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
