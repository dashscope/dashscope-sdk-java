// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.models;

import com.alibaba.dashscope.types.common.ResponseBase;
import java.util.Collections;
import java.util.List;

/**
 * Page of models (Python v2 {@code ModelList} parity). With a {@code provider} filter the items are
 * narrowed client-side while {@link #getTotalCount()} stays the server's pre-filter total.
 */
public final class ModelList extends ResponseBase {
  private final int pageNo;
  private final int pageSize;
  private final int totalCount;
  private final List<Model> models;

  public ModelList(int pageNo, int pageSize, int totalCount, List<Model> models) {
    this.pageNo = pageNo;
    this.pageSize = pageSize;
    this.totalCount = totalCount;
    this.models = models == null ? Collections.emptyList() : Collections.unmodifiableList(models);
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

  public List<Model> getModels() {
    return models;
  }
}
