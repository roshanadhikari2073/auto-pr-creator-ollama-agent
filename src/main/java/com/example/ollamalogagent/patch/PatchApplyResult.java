package com.example.ollamalogagent.patch;

import java.util.List;

public record PatchApplyResult(
    boolean applied,
    List<String> touchedFiles,
    String detail
) {
}
