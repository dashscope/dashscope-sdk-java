// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Keeps the frozen 2.x implementation independent from 3.0 implementation/business packages. */
class LegacyDependencyBoundaryTest {

  @Test
  void legacyMayOnlyImportLegacyOrNeutralSharedPackages() throws IOException {
    Path root = Paths.get("src/main/java/com/alibaba/dashscope/legacy_2x");
    List<String> violations = new ArrayList<>();
    try (Stream<Path> files = Files.walk(root)) {
      files
          .filter(path -> path.toString().endsWith(".java"))
          .forEach(
              path -> {
                try {
                  for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("import com.alibaba.dashscope.")
                        && !trimmed.startsWith("import com.alibaba.dashscope.legacy_2x.")
                        && !trimmed.startsWith("import com.alibaba.dashscope.shared.")) {
                      violations.add(root.relativize(path) + ": " + trimmed);
                    }
                  }
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              });
    }
    assertTrue(violations.isEmpty(), "legacy reverse dependencies: " + violations);
  }
}
