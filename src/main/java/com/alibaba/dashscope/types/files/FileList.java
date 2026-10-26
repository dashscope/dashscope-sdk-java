// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.files;

import com.alibaba.dashscope.types.common.ResponseBase;
import java.util.Collections;
import java.util.List;

/** Page of uploaded files (Python v2 {@code FileList} parity). */
public final class FileList extends ResponseBase {
  private final int pageNo;
  private final int pageSize;
  private final int totalCount;
  private final List<FileObject> files;

  public FileList(int pageNo, int pageSize, int totalCount, List<FileObject> files) {
    this.pageNo = pageNo;
    this.pageSize = pageSize;
    this.totalCount = totalCount;
    this.files = files == null ? Collections.emptyList() : Collections.unmodifiableList(files);
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

  public List<FileObject> getFiles() {
    return files;
  }
}
