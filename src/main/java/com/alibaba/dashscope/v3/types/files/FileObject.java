// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.files;

import com.alibaba.dashscope.v3.types.common.ResponseBase;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One uploaded file (Python v2 {@code File} parity; named {@code FileObject} to stay clear of
 * {@code java.io.File}). Returned on its own by {@code upload}/{@code get} and nested in {@link
 * FileList#getFiles()}; the envelope is filled in on the standalone result and stays empty on the
 * nested copies, because the server reports it once per response rather than once per item.
 */
public final class FileObject extends ResponseBase {
  @JsonProperty("file_id")
  private String fileId;

  @JsonProperty("name")
  private String name;

  @JsonProperty("purpose")
  private String purpose;

  @JsonProperty("size")
  private Long size;

  @JsonProperty("uploaded_at")
  @JsonAlias("upload_time")
  private String uploadedAt;

  public String getFileId() {
    return fileId;
  }

  public String getName() {
    return name;
  }

  public String getPurpose() {
    return purpose;
  }

  public Long getSize() {
    return size;
  }

  public String getUploadedAt() {
    return uploadedAt;
  }
}
