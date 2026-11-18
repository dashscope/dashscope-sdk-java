package com.alibaba.dashscope.tokenizers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

public class TestQwenTokenizerVocabResolution {
  private static final String VOCAB_SHA256 =
      "b2b1b8dfb5cc5f024bafc373121c6aba3f66f9a5a0269e243470a1de16a33186";
  private static final Path CACHED_VOCAB =
      Paths.get(
          System.getProperty("user.home"), ".cache", "dashscope", "tokenizers", "qwen.tiktoken");

  // The vocab is no longer tracked in git; make sure a verified local cache copy
  // exists (classpath copy or ModelScope download) before the env-override test.
  @BeforeAll
  static void ensureVocabCached() throws Exception {
    if (Files.isRegularFile(CACHED_VOCAB)
        && VOCAB_SHA256.equals(QwenTokenizer.sha256(CACHED_VOCAB))) {
      return;
    }
    Files.deleteIfExists(CACHED_VOCAB);
    try (InputStream in = QwenTokenizer.resolveVocab()) {
      if (!Files.isRegularFile(CACHED_VOCAB)) {
        Files.createDirectories(CACHED_VOCAB.getParent());
        Files.copy(in, CACHED_VOCAB, StandardCopyOption.REPLACE_EXISTING);
      }
    }
  }

  private static String sha256(InputStream in) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] buffer = new byte[1 << 20];
    int read;
    while ((read = in.read(buffer)) != -1) {
      digest.update(buffer, 0, read);
    }
    StringBuilder sb = new StringBuilder();
    for (byte b : digest.digest()) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  @Test
  public void testDefaultResolution() throws Exception {
    // classpath copy, verified cache, or ModelScope download
    try (InputStream in = QwenTokenizer.resolveVocab()) {
      assertEquals(VOCAB_SHA256, sha256(in));
    }
  }

  @Test
  @SetEnvironmentVariable(
      key = "DASHSCOPE_QWEN_VOCAB",
      value = "~/.cache/dashscope/tokenizers/qwen.tiktoken")
  public void testEnvOverride() throws Exception {
    try (InputStream in = QwenTokenizer.resolveVocab()) {
      assertEquals(VOCAB_SHA256, sha256(in));
    }
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_QWEN_VOCAB", value = "/nonexistent/qwen.tiktoken")
  public void testEnvOverrideMissingFile() {
    assertThrows(IOException.class, QwenTokenizer::resolveVocab);
  }

  @Test
  public void testExpandHome() {
    Path home = Paths.get(System.getProperty("user.home"));
    assertEquals(home, QwenTokenizer.expandHome("~"));
    assertEquals(
        home.resolve("vocab/qwen.tiktoken"), QwenTokenizer.expandHome("~/vocab/qwen.tiktoken"));
    assertEquals(Paths.get("/tmp/qwen.tiktoken"), QwenTokenizer.expandHome("/tmp/qwen.tiktoken"));
  }

  @Test
  public void testSha256(@TempDir Path tempDir) throws Exception {
    Path file = tempDir.resolve("vocab.txt");
    Files.write(file, new byte[] {'a', 'b', 'c'});
    assertEquals(
        "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
        QwenTokenizer.sha256(file));
  }
}
