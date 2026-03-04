# Ollama Local Log Agent

A Spring Boot backend that simulates a production-like failure, captures logs, asks a local Ollama model for a fix patch, applies the patch in a git workspace, commits it, and opens a GitHub pull request.

## Architecture

```
Application Service
        |
        v
Log Generator (Payment failure simulation)
        |
        v
Log Queue (In-memory BlockingQueue)
        |
        v
AI Agent Service (Orchestrator)
        |-- Ollama (local LLM)
        |-- Git Workspace (JGit)
        |-- GitHub API (PR creation)
                |
                v
            Pull Request
```

Detailed responsibilities are in [docs/ARCHITECTURE.md](/Users/roshanadhikari/Documents/all-spring-applications-here/ollama-local-log-agent/docs/ARCHITECTURE.md).

## Requirements

- Java 17+
- Maven 3.9+
- Ollama running at `http://localhost:11434`
- Optional: GitHub token with `repo` scope for PR creation

## Run

```bash
mvn spring-boot:run
```

Trigger failure:

```bash
curl -i http://localhost:8080/payments/run
```

## Environment Variables

- `SPRING_DATASOURCE_URL` (default: `jdbc:postgresql://localhost:5432/payments`)
- `SPRING_DATASOURCE_USERNAME` (no default)
- `SPRING_DATASOURCE_PASSWORD` (no default)
- `AGENT_WORKSPACE_PATH` (default: current directory)
- `AGENT_OLLAMA_MODEL` (default: `llama3`)
- `AGENT_OLLAMA_BASE_URL` (default: `http://localhost:11434`)
- `AGENT_GIT_PUSH_ENABLED` (default: `false`)
- `AGENT_GITHUB_ENABLED` (default: `false`)
- `AGENT_GITHUB_OWNER`
- `AGENT_GITHUB_REPO`
- `GITHUB_TOKEN` (or `AGENT_GITHUB_TOKEN`)

## Demo Flow

1. `GET /payments/run`
2. Application throws simulated SQL timeout exception.
3. Global exception handler forwards log event to `AgentQueue`.
4. `AgentQueueWorker` consumes the event.
5. `AgentOrchestrator` asks Ollama for a unified git patch.
6. `PatchService` applies the patch (or applies deterministic fallback fix if patch fails).
7. `GitAutomationService` creates branch, commits, and optionally pushes.
8. `GitHubService` opens a pull request.

## Endpoints

- `GET /payments/run` trigger simulated production failure
- `GET /actuator/health` health check
- `GET /actuator/info` app info
