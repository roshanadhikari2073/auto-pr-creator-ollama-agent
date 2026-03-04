package com.example.ollamalogagent.agent;

import com.example.ollamalogagent.agent.model.FixProposal;
import com.example.ollamalogagent.config.AgentProperties;
import com.example.ollamalogagent.logs.LogEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class OllamaClient {

    private final AgentProperties properties;
    private final ObjectMapper objectMapper;

    @Qualifier("ollamaWebClient")
    private final WebClient ollamaWebClient;

    public FixProposal analyze(LogEvent event) {
        String prompt = buildPrompt(event);
        Map<String, Object> requestBody = Map.of(
            "model", properties.getOllama().getModel(),
            "prompt", prompt,
            "stream", false
        );

        try {
            String response = ollamaWebClient.post()
                .uri("/api/generate")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .map(result -> Objects.toString(result.get("response"), ""))
                .timeout(properties.getOllama().getRequestTimeout())
                .block();

            return parseProposal(response);
        } catch (Exception exception) {
            log.error("Failed to analyze log with Ollama", exception);
            return FixProposal.fallback();
        }
    }

    private FixProposal parseProposal(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return FixProposal.fallback();
        }

        String candidate = extractJson(rawResponse);
        try {
            JsonNode node = objectMapper.readTree(candidate);
            return new FixProposal(
                node.path("rootCause").asText("Unknown root cause"),
                node.path("patch").asText(""),
                node.path("branchName").asText("ai-fix"),
                node.path("commitMessage").asText("AI fix for backend timeout"),
                node.path("prTitle").asText("AI agent fix for SQL timeout"),
                node.path("prBody").asText("Generated automatically by local AI agent")
            );
        } catch (JsonProcessingException exception) {
            log.warn("Could not parse Ollama JSON response, falling back");
            return FixProposal.fallback();
        }
    }

    private String extractJson(String response) {
        String trimmed = response.trim();
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1);
        }
        return trimmed;
    }

    private String buildPrompt(LogEvent event) {
        return """
            You are a senior backend reliability engineer.
            Analyze the backend error and return a strict JSON object with this schema:
            {
              "rootCause": "string",
              "patch": "unified git diff patch string",
              "branchName": "ai-fix-short-name",
              "commitMessage": "string",
              "prTitle": "string",
              "prBody": "string"
            }

            Requirements:
            - patch must be a valid unified git patch that can be applied with git apply
            - include file paths relative to repository root
            - avoid markdown code fences
            - keep patch minimal and safe

            Event:
            correlationId=%s
            source=%s
            message=%s
            stackTrace=
            %s
            """.formatted(event.correlationId(), event.source(), event.message(), event.stackTrace());
    }
}
