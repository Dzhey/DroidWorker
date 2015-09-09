package com.be.android.library.worker.demo.ui;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.be.android.library.worker.annotations.OnJobSuccess;
import com.be.android.library.worker.controllers.JobManager;
import com.be.android.library.worker.demo.R;
import com.be.android.library.worker.demo.jobs.LoadListEntryJob;
import com.be.android.library.worker.demo.model.MultiloadDemoEntry;
import com.be.android.library.worker.demo.ui.base.BaseFragment;
import com.be.android.library.worker.demo.ui.base.TitleProvider;
import com.be.android.library.worker.interfaces.Job;
import com.be.android.library.worker.models.LoadJobResult;
import com.be.android.library.worker.util.JobSelector;

import java.util.ArrayList;
import java.util.List;

public class MultiloadWorkDemoFragment extends BaseFragment implements TitleProvider, MultiloadExampleAdapter.ListEntryDataRequestListener {

    private static final String LOG_TAG = MultiloadWorkDemoFragment.class.getName();

    public static final String ARG_ASYNC = "async";

    private static final String TAG_LOADER = "MultiloadWorkDemoFragment_list_entry_loader";
    private static final String KEY_LIST_ENTRY_ID = "MultiloadWorkDemoFragment_list_entry_id";

    private ListView mListView;
    private MultiloadExampleAdapter mAdapter;

    private static String makeLoaderTag(int itemId) {
        return TAG_LOADER + ":" + String.valueOf(itemId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_multiload, container, false);

        mListView = (ListView) view.findViewById(android.R.id.list);

        initListAdapter();

        return view;
    }

    @Override
    public void onDestroyView() {
        // Cancel created jobs as it's result no longer needed
        // By the way: no results will be received by this fragment after onPause()
        // even without this call
        JobManager.getInstance().cancelAll(JobSelector.forJobTags(TAG_LOADER));

        super.onDestroyView();
    }

    private void initListAdapter() {
        mAdapter = new MultiloadExampleAdapter(this);
        List<MultiloadExampleAdapter.Item> items = new ArrayList<MultiloadExampleAdapter.Item>();
        for (int i = 0; i < 20; i++) {
            items.add(new MultiloadExampleAdapter.Item(i));
        }
        mAdapter.setItems(items);
        mListView.setAdapter(mAdapter);
    }

    @Override
    public String getTitle(Resources resources) {
        return resources.getString(R.string.multiload_work_demo);
    }

    @Override
    public void onDataRequested(int itemId) {
        Bundle data = new Bundle();
        data.putInt(KEY_LIST_ENTRY_ID, itemId);
        requestLoad(makeLoaderTag(itemId), data);
        Log.i(LOG_TAG, "Requested data for list entry id:" + String.valueOf(itemId));
    }

    @OnJobSuccess
    public void onItemLoaded(LoadJobResult<MultiloadDemoEntry> result) {
        MultiloadDemoEntry resultData = result.getData();
        mAdapter.setItemData(resultData.getItemId(), resultData.getLoadResult());
    }

    @Override
    public Job onCreateJob(String attachTag, Bundle data) {
        return new LoadListEntryJob(data.getInt(KEY_LIST_ENTRY_ID))
                .setup()
                .group(isAsync() ? JobManager.JOB_GROUP_UNIQUE : JobManager.JOB_GROUP_DEFAULT)
                // 'TAG_LOADER' used to identify jobs created here
                // 'attachTag' will be added to created job automatically
                .addTag(TAG_LOADER)
                .getJob();
    }

    private boolean isAsync() {
        return getArguments() != null && getArguments().getBoolean(ARG_ASYNC, false);
    }
}
