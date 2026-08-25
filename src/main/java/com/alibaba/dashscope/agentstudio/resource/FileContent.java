// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Binary content of a downloaded file.
 *
 * <p>Mirrors the Python SDK's {@code FileContent}: a thin wrapper over the raw bytes with a {@link
 * #writeToFile(Path)} helper, so callers can do {@code
 * client.files().download(fileId).writeToFile("output.txt")}.
 */
public final class FileContent {
  private final byte[] data;

  public FileContent(byte[] data) {
    this.data = data != null ? data : new byte[0];
  }

  /** The raw file bytes. */
  public byte[] getBytes() {
    return data;
  }

  /** Number of bytes. */
  public int length() {
    return data.length;
  }

  /** Write the content to {@code path} (creating parent directories) and return it. */
  public Path writeToFile(String path) throws IOException {
    return writeToFile(Paths.get(path));
  }

  /** Write the content to {@code path} (creating parent directories) and return it. */
  public Path writeToFile(Path path) throws IOException {
    Objects.requireNonNull(path, "path");
    if (path.getParent() != null) {
      Files.createDirectories(path.getParent());
    }
    Files.write(path, data);
    return path;
  }
}
