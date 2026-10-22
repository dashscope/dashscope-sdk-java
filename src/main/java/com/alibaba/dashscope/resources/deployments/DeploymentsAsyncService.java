// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.deployments;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.resources.FlowPublishers;
import com.alibaba.dashscope.types.common.JobStatus;
import com.alibaba.dashscope.types.deployments.Deployment;
import com.alibaba.dashscope.types.deployments.DeploymentCreateParams;
import com.alibaba.dashscope.types.deployments.DeploymentEvent;
import com.alibaba.dashscope.types.deployments.DeploymentList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

/** Async variant of {@link DeploymentsService}; {@code streamEvents} surfaces as a Publisher. */
public final class DeploymentsAsyncService {
  private final DeploymentsService sync;
  private final Executor executor;

  public DeploymentsAsyncService(DeploymentsService sync, Executor executor) {
    this.sync = sync;
    this.executor = executor;
  }

  public CompletableFuture<Deployment> create(DeploymentCreateParams params) {
    return create(params, RequestOptions.NONE);
  }

  public CompletableFuture<Deployment> create(DeploymentCreateParams params, RequestOptions options) {
    return CompletableFuture.supplyAsync(() -> sync.create(params, options), executor);
  }

  public CompletableFuture<Deployment> get(String deployedModel) {
    return get(deployedModel, RequestOptions.NONE);
  }

  public CompletableFuture<Deployment> get(String deployedModel, RequestOptions options) {
    return CompletableFuture.supplyAsync(() -> sync.get(deployedModel, options), executor);
  }

  public CompletableFuture<DeploymentList> list(int pageNo, int pageSize) {
    return list(pageNo, pageSize, RequestOptions.NONE);
  }

  public CompletableFuture<DeploymentList> list(int pageNo, int pageSize, RequestOptions options) {
    return CompletableFuture.supplyAsync(() -> sync.list(pageNo, pageSize, options), executor);
  }

  public CompletableFuture<Deployment> scale(String deployedModel, int capacity) {
    return scale(deployedModel, capacity, RequestOptions.NONE);
  }

  public CompletableFuture<Deployment> scale(
      String deployedModel, int capacity, RequestOptions options) {
    return CompletableFuture.supplyAsync(
        () -> sync.scale(deployedModel, capacity, options), executor);
  }

  public CompletableFuture<JobStatus> delete(String deployedModel) {
    return delete(deployedModel, RequestOptions.NONE);
  }

  public CompletableFuture<JobStatus> delete(String deployedModel, RequestOptions options) {
    return CompletableFuture.supplyAsync(() -> sync.delete(deployedModel, options), executor);
  }

  public Flow.Publisher<DeploymentEvent> streamEvents(String deployedModel) {
    return streamEvents(deployedModel, RequestOptions.NONE);
  }

  public Flow.Publisher<DeploymentEvent> streamEvents(
      String deployedModel, RequestOptions options) {
    return FlowPublishers.fromIterable(() -> sync.streamEvents(deployedModel, options), executor);
  }
}
