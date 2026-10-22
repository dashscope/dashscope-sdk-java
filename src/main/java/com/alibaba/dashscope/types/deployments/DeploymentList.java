// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.deployments;

import com.alibaba.dashscope.types.common.ResponseBase;
import java.util.Collections;
import java.util.List;

/** Page of deployments (Python v2 {@code DeploymentList} parity). */
public final class DeploymentList extends ResponseBase {
  private final int pageNo;
  private final int pageSize;
  private final int totalCount;
  private final List<Deployment> deployments;

  public DeploymentList(int pageNo, int pageSize, int totalCount, List<Deployment> deployments) {
    this.pageNo = pageNo;
    this.pageSize = pageSize;
    this.totalCount = totalCount;
    this.deployments =
        deployments == null ? Collections.emptyList() : Collections.unmodifiableList(deployments);
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

  public List<Deployment> getDeployments() {
    return deployments;
  }
}
