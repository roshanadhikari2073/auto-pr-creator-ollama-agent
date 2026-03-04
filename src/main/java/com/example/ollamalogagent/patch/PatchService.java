package com.example.ollamalogagent.patch;

import com.example.ollamalogagent.config.AgentProperties;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.ApplyResult;
import org.eclipse.jgit.api.Git;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatchService {

    private final AgentProperties properties;

    public PatchApplyResult applyPatch(String patch) {
        if (patch == null || patch.isBlank()) {
            return new PatchApplyResult(false, List.of(), "Patch is empty");
        }

        String normalizedPatch = normalizePatch(patch);
        Path workspace = Path.of(properties.getWorkspacePath()).toAbsolutePath().normalize();

        try (Git git = Git.open(workspace.toFile())) {
            ApplyResult result = git.apply()
                .setPatch(new ByteArrayInputStream(normalizedPatch.getBytes(StandardCharsets.UTF_8)))
                .call();
            return new PatchApplyResult(true, result.getUpdatedFiles().stream().map(file -> file.toPath().toString()).toList(),
                "Patch applied successfully");
        } catch (Exception exception) {
            log.warn("Patch application failed: {}", exception.getMessage());
            return new PatchApplyResult(false, List.of(), exception.getMessage());
        }
    }

    public PatchApplyResult applyFallbackTimeoutFix() {
        Path workspace = Path.of(properties.getWorkspacePath()).toAbsolutePath().normalize();
        Path target = workspace.resolve(properties.getFallbackFixTarget()).normalize();
        if (!Files.exists(target)) {
            return new PatchApplyResult(false, List.of(), "Fallback fix target not found: " + target);
        }

        String fixedService = """
            package com.example.ollamalogagent.payments;

            import lombok.extern.slf4j.Slf4j;
            import org.springframework.stereotype.Service;

            @Slf4j
            @Service
            public class PaymentService {

                public String processPayment() {
                    int maxAttempts = 3;
                    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                        try {
                            simulateDatabaseCall(attempt);
                            return "ok";
                        } catch (RuntimeException exception) {
                            if (attempt == maxAttempts) {
                                throw exception;
                            }
                            log.warn("Payment attempt {} failed: {}", attempt, exception.getMessage());
                        }
                    }
                    return "ok";
                }

                private void simulateDatabaseCall(int attempt) {
                    if (attempt < 3) {
                        throw new RuntimeException("java.sql.SQLTimeoutException: connection timeout after 5s");
                    }
                }
            }
            """;

        try {
            Files.writeString(target, fixedService, StandardCharsets.UTF_8);
            return new PatchApplyResult(true, List.of(target.toString()), "Fallback timeout fix applied");
        } catch (Exception exception) {
            return new PatchApplyResult(false, List.of(), "Fallback fix failed: " + exception.getMessage());
        }
    }

    private String normalizePatch(String patch) {
        String trimmed = patch.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```\\w*\\n", "");
            trimmed = trimmed.replaceFirst("\\n```$", "");
        }
        return trimmed.endsWith("\n") ? trimmed : trimmed + "\n";
    }
}
