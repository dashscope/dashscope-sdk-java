// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.common;

import java.util.List;

/** Paged list result (design §3): {@code pageNo/pageSize} in, this shape out. */
public final class Page<T> extends ResponseBase {
  private final List<T> items;
  private final int totalCount;
  private final int pageNo;
  private final int pageSize;

  public Page(List<T> items, int totalCount, int pageNo, int pageSize) {
    this.items = items;
    this.totalCount = totalCount;
    this.pageNo = pageNo;
    this.pageSize = pageSize;
  }

  public List<T> getItems() {
    return items;
  }

  public int getTotalCount() {
    return totalCount;
  }

  public int getPageNo() {
    return pageNo;
  }

  public int getPageSize() {
    return pageSize;
  }
}
