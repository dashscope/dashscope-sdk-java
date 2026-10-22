// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.files;

import java.nio.file.Path;

/**
 * Parameters for {@code files().upload} (Python v2 parity): a local file plus its purpose.
 * {@code fine-tune}/{@code fine_tune} purposes are validated as JSONL before the upload.
 */
public final class FileUploadParams {
  /** Purpose value the gateway expects for fine-tuning input files. */
  public static final String PURPOSE_FINE_TUNE = "fine-tune";

  private final Path file;
  private final String purpose;
  private final String description;

  private FileUploadParams(Builder b) {
    this.file = b.file;
    this.purpose = b.purpose;
    this.description = b.description;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Path getFile() {
    return file;
  }

  public String getPurpose() {
    return purpose;
  }

  public String getDescription() {
    return description;
  }

  public static final class Builder {
    private Path file;
    private String purpose = PURPOSE_FINE_TUNE;
    private String description;

    private Builder() {}

    public Builder file(Path file) {
      this.file = file;
      return this;
    }

    public Builder purpose(String purpose) {
      this.purpose = purpose;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public FileUploadParams build() {
      if (file == null) {
        throw new IllegalArgumentException("file is required");
      }
      return new FileUploadParams(this);
    }
  }
}
