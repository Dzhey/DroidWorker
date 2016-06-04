package com.be.android.library.worker.demo.net;

import com.be.android.library.worker.demo.net.models.GitHubRepository;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 *
 * Created by Dzhey on 04-Jun-16.
 */
public interface GitHubService {
    @GET("orgs/{company}/repos")
    Call<List<GitHubRepository>> listCompanyRepos(@Path("company") String companyName,
                                                  @Query("page") int page,
                                                  @Query("per_page") int perPage);
}
