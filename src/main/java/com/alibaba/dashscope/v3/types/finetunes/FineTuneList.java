// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.finetunes;

import com.alibaba.dashscope.v3.types.common.ResponseBase;
import java.util.Collections;
import java.util.List;

/** Page of fine-tune jobs (Python v2 {@code FineTuneList} parity). */
public final class FineTuneList extends ResponseBase {
  private final int pageNo;
  private final int pageSize;
  private final int totalCount;
  private final List<FineTune> jobs;

  public FineTuneList(int pageNo, int pageSize, int totalCount, List<FineTune> jobs) {
    this.pageNo = pageNo;
    this.pageSize = pageSize;
    this.totalCount = totalCount;
    this.jobs = jobs == null ? Collections.emptyList() : Collections.unmodifiableList(jobs);
  }

  public int getPageNo() {
    return pageNo;
  }

  public int getPageSize() {
    return pageSize;
  }

  public int getTotalCount() {
    return totalCount;
  }

  public List<FineTune> getJobs() {
    return jobs;
  }
}
