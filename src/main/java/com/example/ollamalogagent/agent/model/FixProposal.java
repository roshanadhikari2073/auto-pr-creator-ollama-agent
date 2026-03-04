package com.example.ollamalogagent.agent.model;

public record FixProposal(
    String rootCause,
    String patch,
    String branchName,
    String commitMessage,
    String prTitle,
    String prBody
) {

    public static FixProposal fallback() {
        return new FixProposal(
            "Could not parse LLM response",
            "",
            "ai-fix",
            "AI fix for backend timeout",
            "AI agent fix for SQL timeout",
            "Generated automatically by local AI agent"
        );
    }
}
