package com.example.ollamalogagent.git;

import com.example.ollamalogagent.config.AgentProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitAutomationService {

    private static final DateTimeFormatter BRANCH_TIMESTAMP =
        DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

    private final AgentProperties properties;

    public GitCommitResult commitAndPush(String branchHint, String commitMessage) {
        Path workspace = Path.of(properties.getWorkspacePath()).toAbsolutePath().normalize();
        if (!Files.exists(workspace.resolve(".git"))) {
            return new GitCommitResult(false, "", "", "Workspace is not a git repository: " + workspace);
        }

        try (Git git = Git.open(workspace.toFile())) {
            checkoutBaseBranch(git);
            String branchName = uniqueBranchName(git.getRepository(), sanitizeBranch(branchHint));

            git.checkout()
                .setCreateBranch(true)
                .setName(branchName)
                .setStartPoint(properties.getGit().getBaseBranch())
                .call();

            git.add().addFilepattern(".").call();
            if (git.status().call().isClean()) {
                return new GitCommitResult(false, branchName, "", "No changes to commit");
            }

            RevCommit commit = git.commit()
                .setMessage(commitMessage)
                .setAuthor("ollama-local-agent", "agent@local")
                .call();

            if (properties.getGit().isPushEnabled()) {
                pushBranch(git, branchName);
            }

            return new GitCommitResult(true, branchName, commit.getName(), "Commit successful");
        } catch (Exception exception) {
            return new GitCommitResult(false, "", "", "Git automation failed: " + exception.getMessage());
        }
    }

    private void checkoutBaseBranch(Git git) throws Exception {
        String baseBranch = properties.getGit().getBaseBranch();
        try {
            git.checkout().setName(baseBranch).call();
        } catch (RefNotFoundException exception) {
            throw new IllegalStateException("Base branch not found: " + baseBranch, exception);
        }
    }

    private String uniqueBranchName(Repository repository, String candidate) throws Exception {
        Set<String> existing = new HashSet<>();
        for (Ref ref : repository.getRefDatabase().getRefsByPrefix("refs/heads/")) {
            existing.add(Repository.shortenRefName(ref.getName()));
        }

        String current = candidate;
        int suffix = 1;
        while (existing.contains(current)) {
            current = candidate + "-" + suffix;
            suffix++;
        }
        return current;
    }

    private String sanitizeBranch(String branchHint) {
        String base = (branchHint == null || branchHint.isBlank()) ? "ai-fix" : branchHint;
        String safe = base.toLowerCase()
            .replaceAll("[^a-z0-9._/-]", "-")
            .replaceAll("-{2,}", "-")
            .replaceAll("^-+", "")
            .replaceAll("-+$", "");
        if (safe.isBlank()) {
            safe = "ai-fix";
        }
        return safe + "-" + BRANCH_TIMESTAMP.format(Instant.now());
    }

    private void pushBranch(Git git, String branchName) throws Exception {
        String token = properties.getGithub().getToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Git push is enabled but GitHub token is not configured");
        }

        Iterable<PushResult> results = git.push()
            .setRemote(properties.getGit().getRemoteName())
            .add(branchName)
            .setCredentialsProvider(new UsernamePasswordCredentialsProvider("x-access-token", token))
            .call();
        for (PushResult result : results) {
            log.info("Push result for {}: {}", branchName, result.getMessages());
        }
    }
}
