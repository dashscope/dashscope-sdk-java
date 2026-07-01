// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.resource;

import com.alibaba.dashscope.agentstudio.AgentStudioConstants;
import com.alibaba.dashscope.agentstudio.model.AgentStudioDeletionStatus;
import com.alibaba.dashscope.agentstudio.model.AgentStudioFile;
import com.alibaba.dashscope.agentstudio.pagination.CursorPage;
import com.alibaba.dashscope.agentstudio.param.FileListParam;
import com.alibaba.dashscope.api.GeneralApi;
import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.common.FlattenResultBase;
import com.alibaba.dashscope.common.GeneralGetParam;
import com.alibaba.dashscope.common.Status;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.protocol.ConnectionOptions;
import com.alibaba.dashscope.protocol.GeneralServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.utils.ApiKey;
import com.alibaba.dashscope.utils.JsonUtils;
import com.alibaba.dashscope.utils.StringUtils;
import com.google.gson.reflect.TypeToken;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class Files implements Closeable {
  private final GeneralApi<HalfDuplexParamBase> api;
  private final String baseUrl;
  private final String apiKey;
  private final OkHttpClient uploadClient;

  public Files(String baseUrl, ConnectionOptions connectionOptions, String apiKey) {
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.api = connectionOptions != null ? new GeneralApi<>(connectionOptions) : new GeneralApi<>();
    this.uploadClient =
        new OkHttpClient.Builder()
            .connectTimeout(AgentStudioConstants.DEFAULT_CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(AgentStudioConstants.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .writeTimeout(AgentStudioConstants.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .build();
  }

  public AgentStudioFile upload(String filePath, String mimeType) {
    return AsyncHelper.joinAndUnwrap(uploadAsync(filePath, mimeType));
  }

  public AgentStudioFile upload(String filename, InputStream inputStream, String mimeType) {
    return AsyncHelper.joinAndUnwrap(uploadAsync(filename, inputStream, mimeType));
  }

  public AgentStudioFile retrieve(String fileId) {
    return retrieve(fileId, null, null);
  }

  public AgentStudioFile retrieve(String fileId, String apiKey, Map<String, String> headers) {
    return AsyncHelper.joinAndUnwrap(retrieveAsync(fileId, apiKey, headers));
  }

  public CursorPage<AgentStudioFile> list(FileListParam param) {
    return AsyncHelper.joinAndUnwrap(listAsync(param));
  }

  public AgentStudioDeletionStatus delete(String fileId) {
    return delete(fileId, null, null);
  }

  public AgentStudioDeletionStatus delete(
      String fileId, String apiKey, Map<String, String> headers) {
    return AsyncHelper.joinAndUnwrap(deleteAsync(fileId, apiKey, headers));
  }

  public CompletableFuture<AgentStudioFile> uploadAsync(String filePath, String mimeType) {
    if (filePath == null || filePath.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("filePath is required!"));
    }
    File file = new File(filePath);
    if (!file.exists() || !file.isFile()) {
      return AsyncHelper.failedFuture(new InputRequiredException("file not found: " + filePath));
    }
    String mt = mimeType != null ? mimeType : guessContentType(filePath);
    RequestBody body = RequestBody.create(MediaType.parse(mt), file);
    return uploadRequestAsync(file.getName(), body);
  }

  public CompletableFuture<AgentStudioFile> uploadAsync(
      String filename, InputStream inputStream, String mimeType) {
    if (filename == null || filename.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("filename is required!"));
    }
    if (inputStream == null) {
      return AsyncHelper.failedFuture(new InputRequiredException("inputStream is required!"));
    }
    String mt = mimeType != null ? mimeType : "application/octet-stream";
    RequestBody body = streamingBody(MediaType.parse(mt), inputStream);
    CompletableFuture<AgentStudioFile> future = uploadRequestAsync(filename, body);
    future.whenComplete(
        (r, t) -> {
          if (t != null) {
            try {
              inputStream.close();
            } catch (IOException e) {
              // ignore
            }
          }
        });
    return future;
  }

  public CompletableFuture<AgentStudioFile> retrieveAsync(String fileId) {
    return retrieveAsync(fileId, null, null);
  }

  public CompletableFuture<AgentStudioFile> retrieveAsync(
      String fileId, String apiKey, Map<String, String> headers) {
    if (fileId == null || fileId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("fileId is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.GET, StringUtils.format("files/%s", fileId), baseUrl);
    String resolvedKey = apiKey != null ? apiKey : this.apiKey;
    return AsyncHelper.callAsync(
            api,
            GeneralGetParam.builder()
                .apiKey(resolvedKey)
                .headers(headers != null ? headers : new HashMap<>())
                .build(),
            opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, AgentStudioFile.class));
  }

  public CompletableFuture<CursorPage<AgentStudioFile>> listAsync(FileListParam param) {
    String query = param.toQueryString();
    String path = query.isEmpty() ? "files" : "files?" + query;
    GeneralServiceOption opt = AgentStudioConstants.newServiceOption(HttpMethod.GET, path, baseUrl);
    return AsyncHelper.callAsync(
            api, GeneralGetParam.builder().apiKey(apiKey).headers(new HashMap<>()).build(), opt)
        .thenApply(
            r -> {
              Type type = new TypeToken<CursorPage<AgentStudioFile>>() {}.getType();
              CursorPage<AgentStudioFile> page = FlattenResultBase.fromDashScopeResult(r, type);
              page.setFetchNext(
                  cursor ->
                      listAsync(
                          FileListParam.builder()
                              .limit(param.getLimit())
                              .scopeId(param.getScopeId())
                              .page(cursor)
                              .build()));
              return page;
            });
  }

  public CompletableFuture<AgentStudioDeletionStatus> deleteAsync(String fileId) {
    return deleteAsync(fileId, null, null);
  }

  public CompletableFuture<AgentStudioDeletionStatus> deleteAsync(
      String fileId, String apiKey, Map<String, String> headers) {
    if (fileId == null || fileId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("fileId is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.DELETE, StringUtils.format("files/%s", fileId), baseUrl);
    String resolvedKey = apiKey != null ? apiKey : this.apiKey;
    return AsyncHelper.callAsync(
            api,
            GeneralGetParam.builder()
                .apiKey(resolvedKey)
                .headers(headers != null ? headers : new HashMap<>())
                .build(),
            opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, AgentStudioDeletionStatus.class));
  }

  private CompletableFuture<AgentStudioFile> uploadRequestAsync(
      String filename, RequestBody fileBody) {
    String key;
    try {
      key = ApiKey.getApiKey(this.apiKey);
    } catch (Exception e) {
      return AsyncHelper.failedFuture(e);
    }
    MultipartBody multipart =
        new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", filename, fileBody)
            .build();
    String resolvedBase = resolveUploadBaseUrl();
    String url = resolvedBase + "/files";
    Request request =
        new Request.Builder()
            .url(url)
            .header("Authorization", "Bearer " + key)
            .post(multipart)
            .build();

    CompletableFuture<AgentStudioFile> future = new CompletableFuture<>();
    uploadClient
        .newCall(request)
        .enqueue(
            new Callback() {
              @Override
              public void onFailure(Call call, IOException e) {
                future.completeExceptionally(new ApiException(e));
              }

              @Override
              public void onResponse(Call call, Response response) {
                try (Response r = response) {
                  String body = r.body() != null ? r.body().string() : "";
                  if (!r.isSuccessful()) {
                    future.completeExceptionally(
                        new ApiException(
                            Status.builder().statusCode(r.code()).message(body).build()));
                    return;
                  }
                  future.complete(
                      JsonUtils.fromJson(body.isEmpty() ? "{}" : body, AgentStudioFile.class));
                } catch (Exception e) {
                  future.completeExceptionally(new ApiException(e));
                }
              }
            });
    return future;
  }

  @Override
  public void close() {
    uploadClient.dispatcher().executorService().shutdown();
    uploadClient.connectionPool().evictAll();
  }

  private String resolveUploadBaseUrl() {
    if (baseUrl != null && !baseUrl.isEmpty()) {
      return baseUrl;
    }
    String envUrl = System.getenv(AgentStudioConstants.ENV_BASE_URL);
    if (envUrl == null || envUrl.isEmpty()) {
      envUrl = System.getenv(AgentStudioConstants.ENV_BASE_URL_ALT);
    }
    if (envUrl != null && !envUrl.isEmpty()) {
      return envUrl;
    }
    return AgentStudioConstants.resolveBaseUrl(null, null);
  }

  private static String guessContentType(String filePath) {
    String lower = filePath.toLowerCase();
    if (lower.endsWith(".zip")) return "application/zip";
    if (lower.endsWith(".pdf")) return "application/pdf";
    if (lower.endsWith(".png")) return "image/png";
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
    if (lower.endsWith(".gif")) return "image/gif";
    if (lower.endsWith(".txt")) return "text/plain";
    if (lower.endsWith(".json")) return "application/json";
    return "application/octet-stream";
  }

  private static RequestBody streamingBody(MediaType mediaType, InputStream inputStream) {
    return new RequestBody() {
      @Override
      public MediaType contentType() {
        return mediaType;
      }

      @Override
      public boolean isOneShot() {
        return true;
      }

      @Override
      public void writeTo(okio.BufferedSink sink) throws java.io.IOException {
        try (InputStream is = inputStream;
            okio.Source source = okio.Okio.source(is)) {
          sink.writeAll(source);
        }
      }
    };
  }
}
