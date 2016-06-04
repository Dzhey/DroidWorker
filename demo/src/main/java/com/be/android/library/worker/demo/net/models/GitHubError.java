package com.be.android.library.worker.demo.net.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

/**
 *
 * Created by Dzhey on 04-Jun-16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubError {

    private String mMessage;
    private String mDocumentationUrl;

    @JsonProperty("message")
    public String getMessage() {
        return mMessage;
    }

    public void setMessage(String message) {
        mMessage = message;
    }

    @JsonProperty("documentation_url")
    public String getDocumentationUrl() {
        return mDocumentationUrl;
    }

    public void setDocumentationUrl(String documentationUrl) {
        mDocumentationUrl = documentationUrl;
    }
}
