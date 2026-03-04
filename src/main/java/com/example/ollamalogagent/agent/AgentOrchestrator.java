package com.example.ollamalogagent.agent;

import com.example.ollamalogagent.agent.model.FixProposal;
import com.example.ollamalogagent.config.AgentProperties;
import com.example.ollamalogagent.git.GitAutomationService;
import com.example.ollamalogagent.git.GitCommitResult;
import com.example.ollamalogagent.github.GitHubService;
import com.example.ollamalogagent.github.PullRequestResult;
import com.example.ollamalogagent.logs.LogEvent;
import com.example.ollamalogagent.patch.PatchApplyResult;
import com.example.ollamalogagent.patch.PatchService;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOrchestrator {

    private final AgentProperties properties;
    private final OllamaClient ollamaClient;
    private final PatchService patchService;
    private final GitAutomationService gitAutomationService;
    private final GitHubService gitHubService;

    public void runPipeline(LogEvent event) {
        Instant start = Instant.now();
        log.info("Pipeline started correlationId={} source={}", event.correlationId(), event.source());

        FixProposal fixProposal = ollamaClient.analyze(event);
        log.info("Root cause inference correlationId={} rootCause={}", event.correlationId(), fixProposal.rootCause());

        PatchApplyResult patchResult = patchService.applyPatch(fixProposal.patch());
        if (!patchResult.applied()) {
            log.info("Applying fallback fix correlationId={} reason={}", event.correlationId(), patchResult.detail());
            patchResult = patchService.applyFallbackTimeoutFix();
        }

        if (!patchResult.applied()) {
            log.warn("Pipeline stopped correlationId={} patch could not be applied detail={}",
                event.correlationId(), patchResult.detail());
            return;
        }

        GitCommitResult commitResult = gitAutomationService.commitAndPush(
            fixProposal.branchName(),
            fixProposal.commitMessage()
        );
        if (!commitResult.committed()) {
            log.info("Pipeline completed with no commit correlationId={} detail={}",
                event.correlationId(), commitResult.detail());
            return;
        }

        PullRequestResult pullRequestResult = gitHubService.openPullRequest(
            fixProposal.prTitle(),
            fixProposal.prBody(),
            commitResult.branchName()
        );

        Duration elapsed = Duration.between(start, Instant.now());
        if (elapsed.compareTo(properties.getPipelineTimeout()) > 0) {
            log.warn("Pipeline exceeded target time correlationId={} elapsedMs={}",
                event.correlationId(), elapsed.toMillis());
        } else {
            log.info("Pipeline completed correlationId={} elapsedMs={} commitId={} pr={}",
                event.correlationId(), elapsed.toMillis(), commitResult.commitId(), pullRequestResult.url());
        }
    }
}
