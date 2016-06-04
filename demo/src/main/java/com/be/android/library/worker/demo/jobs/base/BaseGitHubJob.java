package com.be.android.library.worker.demo.jobs.base;

import com.be.android.library.worker.demo.Consts;
import com.be.android.library.worker.demo.net.GitHubService;
import com.be.android.library.worker.demo.net.models.GitHubError;
import com.be.android.library.worker.jobs.LoadJob;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 *
 * Created by Dzhey on 04-Jun-16.
 */
public abstract class BaseGitHubJob extends LoadJob {

    private Retrofit mRetrofit;
    private ObjectMapper mObjectMapper;
    private GitHubService mGitHubService;

    @Override
    protected void onPreExecute() throws Exception {
        super.onPreExecute();

        // this stuff should be injected
        // but I left it as it is for simplicity
        mObjectMapper = new ObjectMapper();

        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Consts.GITHUB_BASE_URL)
                .addConverterFactory(JacksonConverterFactory.create(mObjectMapper))
                .build();

        mGitHubService = retrofit.create(GitHubService.class);
    }

    public Retrofit getRetrofit() {
        return mRetrofit;
    }

    public ObjectMapper getObjectMapper() {
        return mObjectMapper;
    }

    public GitHubService getGitHubService() {
        return mGitHubService;
    }

    protected GitHubError readGitHubError(Response<?> response) throws IOException {
        return mObjectMapper.readValue(
                response.errorBody().byteStream(),
                GitHubError.class);
    }
}
