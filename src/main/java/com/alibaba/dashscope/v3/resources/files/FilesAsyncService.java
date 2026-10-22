// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.files;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.v3.types.files.DeletionStatus;
import com.alibaba.dashscope.v3.types.files.FileList;
import com.alibaba.dashscope.v3.types.files.FileObject;
import com.alibaba.dashscope.v3.types.files.FileUploadParams;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Async variant of {@link FilesService}. */
public final class FilesAsyncService {
  private final FilesService sync;
  private final Executor executor;

  public FilesAsyncService(FilesService sync, Executor executor) {
    this.sync = sync;
    this.executor = executor;
  }

  public CompletableFuture<FileObject> upload(FileUploadParams params) {
    return upload(params, RequestOptions.NONE);
  }

  public CompletableFuture<FileObject> upload(FileUploadParams params, RequestOptions options) {
    return CompletableFuture.supplyAsync(() -> sync.upload(params, options), executor);
  }

  public CompletableFuture<FileList> list(int pageNo, int pageSize) {
    return list(pageNo, pageSize, RequestOptions.NONE);
  }

  public CompletableFuture<FileList> list(int pageNo, int pageSize, RequestOptions options) {
    return CompletableFuture.supplyAsync(() -> sync.list(pageNo, pageSize, options), executor);
  }

  public CompletableFuture<FileObject> get(String fileId) {
    return get(fileId, RequestOptions.NONE);
  }

  public CompletableFuture<FileObject> get(String fileId, RequestOptions options) {
    return CompletableFuture.supplyAsync(() -> sync.get(fileId, options), executor);
  }

  public CompletableFuture<DeletionStatus> delete(String fileId) {
    return delete(fileId, RequestOptions.NONE);
  }

  public CompletableFuture<DeletionStatus> delete(String fileId, RequestOptions options) {
    return CompletableFuture.supplyAsync(() -> sync.delete(fileId, options), executor);
  }
}
