package com.example.ollamalogagent.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    private String workspacePath = System.getProperty("user.dir");
    private Duration pipelineTimeout = Duration.ofSeconds(55);
    private String fallbackFixTarget = "src/main/java/com/example/ollamalogagent/payments/PaymentService.java";
    private Ollama ollama = new Ollama();
    private Git git = new Git();
    private Github github = new Github();

    @Getter
    @Setter
    public static class Ollama {

        private String baseUrl = "http://localhost:11434";
        private String model = "llama3";
        private Duration requestTimeout = Duration.ofSeconds(25);
    }

    @Getter
    @Setter
    public static class Git {

        private String remoteName = "origin";
        private String baseBranch = "main";
        private boolean pushEnabled = false;
    }

    @Getter
    @Setter
    public static class Github {

        private String apiBaseUrl = "https://api.github.com";
        private boolean enabled = false;
        private String owner = "";
        private String repo = "";
        private String token = "";
    }
}
