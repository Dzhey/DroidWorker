package com.be.android.library.worker.demo.ui;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.be.android.library.worker.annotations.OnJobSuccess;
import com.be.android.library.worker.controllers.JobManager;
import com.be.android.library.worker.demo.R;
import com.be.android.library.worker.demo.jobs.LoadListEntriesJob;
import com.be.android.library.worker.demo.model.MultiloadDemoEntry;
import com.be.android.library.worker.demo.ui.base.BaseFragment;
import com.be.android.library.worker.demo.ui.base.TitleProvider;
import com.be.android.library.worker.interfaces.Job;
import com.be.android.library.worker.models.LoadJobResult;

import java.util.ArrayList;
import java.util.List;

public class ForkJoinLoadDemoFragment extends BaseFragment implements TitleProvider {

    private static final String LOG_TAG = ForkJoinLoadDemoFragment.class.getName();

    private static final String TAG_LOADER = "ForkJoinLoadDemoFragment_list_entry_loader";

    private ListView mListView;
    private MultiloadExampleAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_multiload, container, false);

        mListView = (ListView) view.findViewById(android.R.id.list);

        initListAdapter();

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requestLoad(TAG_LOADER);
    }

    private void initListAdapter() {
        mAdapter = new MultiloadExampleAdapter(null);
        List<MultiloadExampleAdapter.Item> items = new ArrayList<MultiloadExampleAdapter.Item>();
        for (int i = 0; i < 20; i++) {
            items.add(new MultiloadExampleAdapter.Item(i));
        }
        mAdapter.setItems(items);
        mListView.setAdapter(mAdapter);
    }

    @Override
    public String getTitle(Resources resources) {
        return resources.getString(R.string.fork_join_example);
    }

    @OnJobSuccess
    public void onItemLoadsed(LoadJobResult<List<MultiloadDemoEntry>> result) {
        List<MultiloadDemoEntry> resultData = result.getData();

        for (MultiloadDemoEntry entry : resultData) {
            mAdapter.setItemData(entry.getItemId(), entry.getLoadResult());
        }
    }

    @Override
    public Job onCreateJob(String attachTag) {
        return new LoadListEntriesJob()
                .setup()
                .group(JobManager.JOB_GROUP_UNIQUE)
                .getJob();
    }
}
