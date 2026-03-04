package com.example.ollamalogagent.queue;

import com.example.ollamalogagent.logs.LogEvent;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AgentQueue {

    private final BlockingQueue<LogEvent> queue = new LinkedBlockingQueue<>();

    public void publish(LogEvent event) {
        queue.offer(event);
        log.info("Queued log event correlationId={} queueSize={}", event.correlationId(), queue.size());
    }

    public LogEvent take() throws InterruptedException {
        return queue.take();
    }
}
