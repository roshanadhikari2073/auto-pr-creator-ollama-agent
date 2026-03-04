package com.example.ollamalogagent.git;

public record GitCommitResult(
    boolean committed,
    String branchName,
    String commitId,
    String detail
) {
}
