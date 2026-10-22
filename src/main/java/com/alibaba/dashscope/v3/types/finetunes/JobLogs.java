// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.finetunes;

import com.alibaba.dashscope.v3.types.common.ResponseBase;

/**
 * Training logs of one job, from {@code fineTunes().logs} (Python v2 {@code JobLogs} parity).
 * The server may return logs either as a single string or as an array of lines; the array form is
 * normalized into newline-joined text so {@link #getContent()} stays a plain string.
 */
public final class JobLogs extends ResponseBase {
  private final String jobId;
  private final String content;

  public JobLogs(String jobId, String content) {
    this.jobId = jobId;
    this.content = content == null ? "" : content;
  }

  public String getJobId() {
    return jobId;
  }

  public String getContent() {
    return content;
  }
}
