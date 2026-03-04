# System Architecture

## Goal

Build a realistic backend workflow where runtime failures are converted into automated code fixes through a local LLM and developer automation.

## Components and Responsibilities

### 1) Application Service

- Hosts business endpoints.
- Triggers payment workflow that may fail.
- Exposes operational endpoints via Actuator.

### 2) Log Generator

- `PaymentService` intentionally throws `SQLTimeoutException` style failure.
- Produces stack traces that resemble production incidents.

### 3) Log Collector

- `GlobalExceptionHandler` catches request failures.
- `LogCollector` transforms exceptions into structured `LogEvent`.
- Publishes events to `AgentQueue`.

### 4) Log Queue (In-memory)

- `AgentQueue` stores `LogEvent` items in `LinkedBlockingQueue`.
- `AgentQueueWorker` continuously consumes queue items on a dedicated thread.
- Decouples runtime traffic from AI-agent work.

### 5) AI Agent Service (Orchestrator)

- `AgentOrchestrator` executes pipeline for each `LogEvent`.
- Calls local LLM (`OllamaClient`) for root-cause + patch generation.
- Uses `PatchService` to apply generated patch.
- Uses `GitAutomationService` (JGit) to branch, stage, commit, and push.
- Uses `GitHubService` to open pull request.

### 6) Local LLM Integration (Ollama)

- HTTP: `POST /api/generate`.
- Prompt demands strict JSON with:
  - root cause
  - unified git patch
  - commit message
  - PR title/body
  - branch hint

### 7) Git Workspace Automation

- Workspace path configurable (`agent.workspace-path`).
- Branch naming: deterministic prefix + timestamp.
- Commit only when effective file changes exist.

### 8) GitHub Pull Request Automation

- API: `POST /repos/{owner}/{repo}/pulls`.
- Enabled only if `agent.github.enabled=true` and token is set.

## Sequence

1. `GET /payments/run`
2. `PaymentService` throws timeout exception.
3. `GlobalExceptionHandler` captures exception and emits `LogEvent`.
4. `AgentQueueWorker` picks up event.
5. `OllamaClient` asks local model for patch.
6. `PatchService` applies patch to workspace.
7. `GitAutomationService` creates `ai-fix-*` branch and commits.
8. `GitHubService` opens PR.

## Operational Constraints

- Pipeline target runtime: < 1 minute.
- Ollama and pipeline calls have explicit timeouts.
- If LLM patch is invalid, deterministic fallback fix is attempted for known timeout pattern.
