package com.be.android.library.worker.demo.jobs.models;

import com.be.android.library.worker.demo.net.models.GitHubError;
import com.be.android.library.worker.models.JobResultStatus;
import com.be.android.library.worker.models.LoadJobResult;

/**
 *
 * Created by Dzhey on 04-Jun-16.
 */
public class GitHubErrorResult<T> extends LoadJobResult<T> {

    private final GitHubError mGitHubError;

    public GitHubErrorResult(GitHubError gitHubError) {
        super(JobResultStatus.FAILED);

        mGitHubError = gitHubError;
    }

    public GitHubError getGitHubError() {
        return mGitHubError;
    }
}
