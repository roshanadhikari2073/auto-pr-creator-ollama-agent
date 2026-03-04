package com.example.ollamalogagent.github;

public record PullRequestResult(
    boolean created,
    String url,
    String detail
) {
}
