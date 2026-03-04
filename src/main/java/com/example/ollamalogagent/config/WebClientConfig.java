package com.example.ollamalogagent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean("ollamaWebClient")
    public WebClient ollamaWebClient(AgentProperties properties) {
        return WebClient.builder()
            .baseUrl(properties.getOllama().getBaseUrl())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    @Bean("githubWebClient")
    public WebClient githubWebClient(AgentProperties properties) {
        return WebClient.builder()
            .baseUrl(properties.getGithub().getApiBaseUrl())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }
}
