package com.be.android.library.worker.demo.jobs;

import android.util.Log;

import com.be.android.library.worker.demo.jobs.base.BaseGitHubJob;
import com.be.android.library.worker.demo.jobs.models.GitHubErrorResult;
import com.be.android.library.worker.demo.net.models.GitHubRepository;
import com.be.android.library.worker.models.LoadJobResult;
import com.be.library.worker.annotations.JobExtra;

import java.util.List;

import retrofit2.Response;

/**
 *
 * Created by Dzhey on 04-Jun-16.
 */
public class LoadGitHubRepoListJob extends BaseGitHubJob {

    private static final String LOG_TAG = LoadGitHubRepoListJob.class.getName();

    private static final String COMPANY_NAME_DEFAULT = "googlesamples";
    private static final int PER_PAGE = 30;

    @JobExtra(optional = true)
    int mRequestedPage = 1;

    @JobExtra(optional = true)
    String mCompanyName = COMPANY_NAME_DEFAULT;

    @Override
    protected void onPreExecute() throws Exception {
        super.onPreExecute();

        LoadGitHubRepoListJobExtras.injectExtras(this);

        if (mRequestedPage < 1) {
            throw new IllegalArgumentException("page cannot be less than 1");
        }

        Log.d(LOG_TAG, "loading GitHub repo page " + String.valueOf(mRequestedPage) + "..");
        Thread.sleep(1000);
    }

    @Override
    protected LoadJobResult<List<GitHubRepository>> performLoad() throws Exception {
        final Response<List<GitHubRepository>> response = getGitHubService().listCompanyRepos(
                mCompanyName, mRequestedPage, PER_PAGE).execute();

        if (!response.isSuccessful()) {
            Log.w(LOG_TAG, "failed to load repo list: " + response.message());

            return new GitHubErrorResult<>(readGitHubError(response));
        }

        Log.d(LOG_TAG, "successfully loaded GitHub repo list page "
                + String.valueOf(mRequestedPage));

        return new LoadJobResult<>(response.body());
    }
}
