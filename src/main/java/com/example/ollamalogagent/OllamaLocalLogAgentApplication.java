package com.example.ollamalogagent;

import com.example.ollamalogagent.config.AgentProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AgentProperties.class)
public class OllamaLocalLogAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(OllamaLocalLogAgentApplication.class, args);
    }
}
