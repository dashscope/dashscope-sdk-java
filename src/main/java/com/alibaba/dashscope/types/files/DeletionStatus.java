// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.files;

import com.alibaba.dashscope.types.common.ResponseBase;

/** Outcome of {@code files().delete} (Python v2 {@code DeletionStatus} parity). */
public final class DeletionStatus extends ResponseBase {
  private final boolean deleted;

  public DeletionStatus(boolean deleted) {
    this.deleted = deleted;
  }

  public boolean isDeleted() {
    return deleted;
  }
}
