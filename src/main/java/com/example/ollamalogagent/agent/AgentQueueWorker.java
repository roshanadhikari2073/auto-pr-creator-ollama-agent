package com.example.ollamalogagent.agent;

import com.example.ollamalogagent.logs.LogEvent;
import com.example.ollamalogagent.queue.AgentQueue;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentQueueWorker {

    private final AgentQueue agentQueue;
    private final AgentOrchestrator orchestrator;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("agent-queue-worker");
        thread.setDaemon(true);
        return thread;
    });

    @PostConstruct
    public void start() {
        worker.submit(this::consumeForever);
    }

    @PreDestroy
    public void stop() {
        worker.shutdownNow();
    }

    private void consumeForever() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                LogEvent event = agentQueue.take();
                orchestrator.runPipeline(event);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (Exception exception) {
                log.error("Pipeline failed while processing queue item", exception);
            }
        }
    }
}
