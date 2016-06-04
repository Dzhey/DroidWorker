package com.be.android.library.worker.demo.net.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

/**
 *
 * Created by Dzhey on 04-Jun-16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubRepository {

    private long mId;
    private String mName;
    private String mLanguage;
    private Date mUpdatedAt;

    @JsonProperty("id")
    public long getId() {
        return mId;
    }

    public void setId(long id) {
        mId = id;
    }

    @JsonProperty("name")
    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    @JsonProperty("language")
    public String getLanguage() {
        return mLanguage;
    }

    public void setLanguage(String language) {
        mLanguage = language;
    }

    @JsonProperty("updated_at")
    public Date getUpdatedAt() {
        return mUpdatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        mUpdatedAt = updatedAt;
    }
}
