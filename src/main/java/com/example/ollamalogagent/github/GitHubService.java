package com.example.ollamalogagent.github;

import com.example.ollamalogagent.config.AgentProperties;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubService {

    private final AgentProperties properties;

    @Qualifier("githubWebClient")
    private final WebClient githubWebClient;

    public PullRequestResult openPullRequest(String title, String body, String branchName) {
        if (!properties.getGithub().isEnabled()) {
            return new PullRequestResult(false, "", "GitHub integration disabled");
        }

        if (properties.getGithub().getOwner().isBlank() || properties.getGithub().getRepo().isBlank()) {
            return new PullRequestResult(false, "", "Missing GitHub owner/repo");
        }

        if (properties.getGithub().getToken().isBlank()) {
            return new PullRequestResult(false, "", "Missing GitHub token");
        }

        Map<String, Object> bodyPayload = Map.of(
            "title", title,
            "head", branchName,
            "base", properties.getGit().getBaseBranch(),
            "body", body
        );

        try {
            Map<String, Object> response = githubWebClient.post()
                .uri("/repos/{owner}/{repo}/pulls", properties.getGithub().getOwner(), properties.getGithub().getRepo())
                .headers(headers -> headers.setBearerAuth(properties.getGithub().getToken()))
                .bodyValue(bodyPayload)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .timeout(Duration.ofSeconds(20))
                .block();

            String url = Objects.toString(response.get("html_url"), "");
            return new PullRequestResult(true, url, "Pull request opened");
        } catch (Exception exception) {
            log.error("Could not open pull request", exception);
            return new PullRequestResult(false, "", "PR creation failed: " + exception.getMessage());
        }
    }
}
