// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Keeps the frozen 2.x implementation independent from 3.0 implementation/business packages. */
class LegacyDependencyBoundaryTest {

  /**
   * Matches any root-package reference EXCEPT the allowed {@code legacy_2x} and {@code shared}
   * prefixes. The trailing-dot/word-boundary lookahead keeps a hypothetical {@code
   * com.alibaba.dashscope.sharedXxx} package from being silently whitelisted by a plain-prefix
   * strip, while still allowing {@code legacy_2x.*}/{@code shared.*} wildcard imports and package
   * declarations.
   */
  private static final Pattern FORBIDDEN =
      Pattern.compile("com\\.alibaba\\.dashscope\\.(?!legacy_2x($|\\.)|shared($|\\.))");

  @Test
  void legacyMayOnlyReferenceLegacyOrNeutralSharedPackages() throws IOException {
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
                    if (!isComment(trimmed) && FORBIDDEN.matcher(trimmed).find()) {
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

  @Test
  void legacyPublicTopLevelTypesAreDeprecated() throws IOException, ClassNotFoundException {
    Path root = Paths.get("src/main/java");
    List<String> violations = new ArrayList<>();
    int publicTypes = 0;
    try (Stream<Path> files = Files.walk(root.resolve("com/alibaba/dashscope/legacy_2x"))) {
      List<Path> sources =
          files
              .filter(path -> path.toString().endsWith(".java"))
              .filter(path -> !path.getFileName().toString().equals("package-info.java"))
              .collect(Collectors.toList());
      for (Path source : sources) {
        String relative = root.relativize(source).toString();
        String className =
            relative
                .substring(0, relative.length() - ".java".length())
                .replace(File.separatorChar, '.');
        Class<?> type = Class.forName(className, false, getClass().getClassLoader());
        if (Modifier.isPublic(type.getModifiers()) && type.getEnclosingClass() == null) {
          publicTypes++;
          if (!type.isAnnotationPresent(Deprecated.class)) {
            violations.add(className);
          }
        }
      }
    }
    assertTrue(publicTypes > 0, "no legacy public top-level types checked");
    assertTrue(
        violations.isEmpty(), "legacy public top-level types missing @Deprecated: " + violations);
  }

  /**
   * Javadoc, block comments and line comments do not create compile-time dependencies; the scan
   * below matches the inline-FQCN pattern too (e.g. {@code new com.alibaba.dashscope.Foo()} with no
   * import statement), which an import-only check would miss.
   */
  private static boolean isComment(String trimmedLine) {
    return trimmedLine.startsWith("*")
        || trimmedLine.startsWith("//")
        || trimmedLine.startsWith("/*");
  }
}
