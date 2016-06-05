package com.be.android.library.worker.demo.ui;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.be.android.library.worker.annotations.OnJobFailure;
import com.be.android.library.worker.annotations.OnJobSuccess;
import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.demo.R;
import com.be.android.library.worker.demo.jobs.LoadGitHubRepoListJob;
import com.be.android.library.worker.demo.jobs.LoadGitHubRepoListJobExtras;
import com.be.android.library.worker.demo.jobs.models.GitHubErrorResult;
import com.be.android.library.worker.demo.net.models.GitHubError;
import com.be.android.library.worker.demo.net.models.GitHubRepository;
import com.be.android.library.worker.demo.ui.base.BaseFragment;
import com.be.android.library.worker.demo.ui.base.TitleProvider;
import com.be.android.library.worker.interfaces.Job;
import com.be.android.library.worker.models.LoadJobResult;
import com.be.android.library.worker.util.JobSelector;

import java.util.ArrayList;
import java.util.List;

public class GitHubDemoFragment extends BaseFragment implements TitleProvider {

    private static final String LOG_TAG = GitHubDemoFragment.class.getName();

    private static final String TAG_LOAD_JOB = "GitHubDemoFragment_Loader";
    private static final int LOAD_MORE_THRESHOLD = 15;
    private static final String KEY_PAGE_NUM = "page_num";

    private Button mButtonTryAgain;
    private ProgressBar mProgressBar;
    private ListView mListView;
    private List<GitHubRepository> mRepoList;
    private Bundle mLoaderBundle;
    private int mCurrentPage;
    private View mLoadMoreProgressView;
    private boolean mIsLoadedLastPage;

    private final BaseAdapter mRepoListAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return mRepoList.size();
        }

        @Override
        public GitHubRepository getItem(int position) {
            return mRepoList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final GitHubRepository item = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity())
                        .inflate(R.layout.view_github_repository_item, parent, false);
            }

            ((TextView) convertView.findViewById(R.id.repoName)).setText(item.getName());
            ((TextView) convertView.findViewById(R.id.repoLang)).setText(item.getLanguage());

            return convertView;
        }
    };

    private final AbsListView.OnScrollListener mListScrollListener =
            new AbsListView.OnScrollListener() {

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                             int visibleItemCount, int totalItemCount) {

            if (totalItemCount - firstVisibleItem <= LOAD_MORE_THRESHOLD) {
                requestLoadNextPage();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLoaderBundle = new Bundle();

        getProgressTracker().addSelector(JobSelector.forJobTags(TAG_LOAD_JOB));
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        mLoadMoreProgressView = inflater.inflate(R.layout.view_progress_bar, mListView, false);

        final View view = inflater.inflate(R.layout.fragment_github_demo, container, false);

        mRepoList = new ArrayList<>();
        mButtonTryAgain = (Button) view.findViewById(R.id.buttonTryAgain);
        mProgressBar = (ProgressBar) view.findViewById(android.R.id.progress);
        mListView = (ListView) view.findViewById(android.R.id.list);
        mListView.setAdapter(mRepoListAdapter);
        mListView.addFooterView(mLoadMoreProgressView);
        mListView.setOnScrollListener(mListScrollListener);
        mLoadMoreProgressView.setVisibility(View.GONE);
        mButtonTryAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestLoadRetry();
            }
        });

        return view;
    }

    @Override
    protected void onJobProgressStarted() {
        if (mCurrentPage < 1) {
            return;
        }

        mLoadMoreProgressView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onJobProgressStopped() {
        mProgressBar.setVisibility(View.GONE);
        mLoadMoreProgressView.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();

        requestLoadNextPage();
    }

    @Override
    public void onDestroyView() {
        mRepoList.clear();
        mCurrentPage = 1;

        super.onDestroyView();
    }

    @Override
    public Job onCreateJob(String attachTag, Bundle args) {
        final int requestedPage = args.getInt(KEY_PAGE_NUM);

        Log.i(LOG_TAG, "requested to load repo list page: " + String.valueOf(requestedPage));

        return new LoadGitHubRepoListJob()
                .setup()
                .configure(LoadGitHubRepoListJobExtras.captureExtras()
                        .setRequestedPage(requestedPage)
                        .apply())
                .getJob();
    }

    @OnJobSuccess(LoadGitHubRepoListJob.class)
    public void onRepoListLoaded(LoadJobResult<List<GitHubRepository>> result) {
        if (result.getData().isEmpty()) {
            showToast("All pages are loaded");
            Log.i(LOG_TAG, "loaded last repo list page");
            mIsLoadedLastPage = true;
            mListView.removeFooterView(mLoadMoreProgressView);
            return;
        }

        mCurrentPage = (int) result.getJobParams()
                .getExtra(LoadGitHubRepoListJobExtras.EXTRA_REQUESTED_PAGE);
        mRepoList.addAll(result.getData());
        mRepoListAdapter.notifyDataSetChanged();
    }

    @OnJobFailure(LoadGitHubRepoListJob.class)
    public void onRepoListLoadFailure(GitHubErrorResult failureResult) {
        mIsLoadedLastPage = true;
        mButtonTryAgain.setVisibility(View.VISIBLE);

        final GitHubError error = failureResult.getGitHubError();
        showToast(String.valueOf(error.getMessage()), Toast.LENGTH_LONG);
    }

    @OnJobFailure(LoadGitHubRepoListJob.class)
    public void onRepoListLoadFailure() {
        mIsLoadedLastPage = true;
        mButtonTryAgain.setVisibility(View.VISIBLE);

        showToast("Load failed for some reason; Please see logcat logs.", Toast.LENGTH_LONG);
    }

    @Override
    public boolean handleBackPress() {
        getJobManager().cancelAll(JobSelector.forJobTags(TAG_LOAD_JOB));

        return super.handleBackPress();
    }

    @Override
    public String getTitle(Resources resources) {
        return resources.getString(R.string.retrofit_demo);
    }

    private void requestLoadNextPage() {
        if (mIsLoadedLastPage) {
            return;
        }

        mLoaderBundle.putInt(KEY_PAGE_NUM, mCurrentPage + 1);

        requestLoad(TAG_LOAD_JOB, mLoaderBundle);
    }

    private void requestLoadRetry() {
        mIsLoadedLastPage = false;
        mButtonTryAgain.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
        requestLoadNextPage();
    }
}
