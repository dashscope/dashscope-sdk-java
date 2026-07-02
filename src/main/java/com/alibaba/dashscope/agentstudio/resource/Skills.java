// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.resource;

import com.alibaba.dashscope.agentstudio.AgentStudioConstants;
import com.alibaba.dashscope.agentstudio.model.AgentStudioDeletionStatus;
import com.alibaba.dashscope.agentstudio.model.Skill;
import com.alibaba.dashscope.agentstudio.model.SkillVersion;
import com.alibaba.dashscope.agentstudio.pagination.CursorPage;
import com.alibaba.dashscope.agentstudio.param.SkillCreateParam;
import com.alibaba.dashscope.agentstudio.param.SkillListParam;
import com.alibaba.dashscope.api.GeneralApi;
import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.common.FlattenResultBase;
import com.alibaba.dashscope.common.GeneralGetParam;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.protocol.ConnectionOptions;
import com.alibaba.dashscope.protocol.GeneralServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.utils.StringUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.io.Closeable;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class Skills implements Closeable {
  private final GeneralApi<HalfDuplexParamBase> api;
  private final String baseUrl;
  private final String apiKey;
  private final Files files;

  public Skills(String baseUrl, ConnectionOptions connectionOptions, String apiKey, Files files) {
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.api = connectionOptions != null ? new GeneralApi<>(connectionOptions) : new GeneralApi<>();
    this.files = files;
  }

  private CompletableFuture<String> resolveFileId(SkillCreateParam param) {
    if (param == null) {
      return AsyncHelper.failedFuture(new InputRequiredException("param is required!"));
    }
    if (param.getFileId() != null) {
      return CompletableFuture.completedFuture(param.getFileId());
    }
    if (param.getFile() != null) {
      if (files == null) {
        return AsyncHelper.failedFuture(
            new InputRequiredException(
                "File upload requires a configured Skills instance (use AgentStudioClient)"));
      }
      return files.uploadAsync(param.getFile(), param.getMimeType()).thenApply(f -> f.getId());
    }
    return AsyncHelper.failedFuture(
        new InputRequiredException("Either fileId or file must be provided"));
  }

  public Skill create(SkillCreateParam param) {
    return AsyncHelper.joinAndUnwrap(createAsync(param));
  }

  public Skill retrieve(String skillId) {
    return retrieve(skillId, null, null);
  }

  public Skill retrieve(String skillId, String apiKey, Map<String, String> headers) {
    return AsyncHelper.joinAndUnwrap(retrieveAsync(skillId, apiKey, headers));
  }

  public CursorPage<Skill> list(SkillListParam param) {
    return AsyncHelper.joinAndUnwrap(listAsync(param));
  }

  public AgentStudioDeletionStatus delete(String skillId) {
    return delete(skillId, null, null);
  }

  public AgentStudioDeletionStatus delete(
      String skillId, String apiKey, Map<String, String> headers) {
    return AsyncHelper.joinAndUnwrap(deleteAsync(skillId, apiKey, headers));
  }

  public SkillVersion createVersion(String skillId, SkillCreateParam param) {
    return AsyncHelper.joinAndUnwrap(createVersionAsync(skillId, param));
  }

  public CursorPage<SkillVersion> listVersions(String skillId, SkillListParam param) {
    return AsyncHelper.joinAndUnwrap(listVersionsAsync(skillId, param));
  }

  public SkillVersion retrieveVersion(String skillId, String version) {
    return AsyncHelper.joinAndUnwrap(retrieveVersionAsync(skillId, version));
  }

  public JsonObject downloadVersion(String skillId, String version) {
    return AsyncHelper.joinAndUnwrap(downloadVersionAsync(skillId, version));
  }

  public CompletableFuture<Skill> createAsync(SkillCreateParam param) {
    return resolveFileId(param)
        .thenCompose(
            fileId -> {
              param.setFileId(fileId);
              GeneralServiceOption opt =
                  AgentStudioConstants.newServiceOption(HttpMethod.POST, "skills", baseUrl);
              return AsyncHelper.callAsync(
                  api, AgentStudioConstants.withApiKey(apiKey, param), opt);
            })
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Skill.class));
  }

  public CompletableFuture<Skill> retrieveAsync(String skillId) {
    return retrieveAsync(skillId, null, null);
  }

  public CompletableFuture<Skill> retrieveAsync(
      String skillId, String apiKey, Map<String, String> headers) {
    if (skillId == null || skillId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("skillId is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.GET, StringUtils.format("skills/%s", skillId), baseUrl);
    String resolvedKey = apiKey != null ? apiKey : this.apiKey;
    return AsyncHelper.callAsync(
            api,
            GeneralGetParam.builder()
                .apiKey(resolvedKey)
                .headers(headers != null ? headers : new HashMap<>())
                .build(),
            opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Skill.class));
  }

  public CompletableFuture<CursorPage<Skill>> listAsync(SkillListParam param) {
    if (param == null) {
      return AsyncHelper.failedFuture(new InputRequiredException("param is required!"));
    }
    String query = param.toQueryString();
    String path = query.isEmpty() ? "skills" : "skills?" + query;
    GeneralServiceOption opt = AgentStudioConstants.newServiceOption(HttpMethod.GET, path, baseUrl);
    return AsyncHelper.callAsync(
            api, GeneralGetParam.builder().apiKey(apiKey).headers(new HashMap<>()).build(), opt)
        .thenApply(
            r -> {
              Type type = new TypeToken<CursorPage<Skill>>() {}.getType();
              CursorPage<Skill> page = FlattenResultBase.fromDashScopeResult(r, type);
              page.setFetchNext(
                  cursor ->
                      listAsync(
                          SkillListParam.builder()
                              .source(param.getSource())
                              .limit(param.getLimit())
                              .page(cursor)
                              .build()));
              return page;
            });
  }

  public CompletableFuture<AgentStudioDeletionStatus> deleteAsync(String skillId) {
    return deleteAsync(skillId, null, null);
  }

  public CompletableFuture<AgentStudioDeletionStatus> deleteAsync(
      String skillId, String apiKey, Map<String, String> headers) {
    if (skillId == null || skillId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("skillId is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.DELETE, StringUtils.format("skills/%s", skillId), baseUrl);
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

  public CompletableFuture<SkillVersion> createVersionAsync(
      String skillId, SkillCreateParam param) {
    if (skillId == null || skillId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("skillId is required!"));
    }
    return resolveFileId(param)
        .thenCompose(
            fileId -> {
              param.setFileId(fileId);
              GeneralServiceOption opt =
                  AgentStudioConstants.newServiceOption(
                      HttpMethod.POST, StringUtils.format("skills/%s/versions", skillId), baseUrl);
              return AsyncHelper.callAsync(
                  api, AgentStudioConstants.withApiKey(apiKey, param), opt);
            })
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, SkillVersion.class));
  }

  public CompletableFuture<CursorPage<SkillVersion>> listVersionsAsync(
      String skillId, SkillListParam param) {
    if (skillId == null || skillId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("skillId is required!"));
    }
    String query = param != null ? param.toQueryString() : "";
    String path = StringUtils.format("skills/%s/versions", skillId);
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.GET, query.isEmpty() ? path : path + "?" + query, baseUrl);
    return AsyncHelper.callAsync(
            api, GeneralGetParam.builder().apiKey(apiKey).headers(new HashMap<>()).build(), opt)
        .thenApply(
            r -> {
              Type type = new TypeToken<CursorPage<SkillVersion>>() {}.getType();
              CursorPage<SkillVersion> page = FlattenResultBase.fromDashScopeResult(r, type);
              page.setFetchNext(
                  cursor ->
                      listVersionsAsync(
                          skillId,
                          SkillListParam.builder()
                              .source(param != null ? param.getSource() : null)
                              .limit(param != null ? param.getLimit() : null)
                              .page(cursor)
                              .build()));
              return page;
            });
  }

  public CompletableFuture<SkillVersion> retrieveVersionAsync(String skillId, String version) {
    if (skillId == null || skillId.isEmpty() || version == null || version.isEmpty()) {
      return AsyncHelper.failedFuture(
          new InputRequiredException("skillId and version are required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.GET, StringUtils.format("skills/%s/versions/%s", skillId, version), baseUrl);
    return AsyncHelper.callAsync(
            api, GeneralGetParam.builder().apiKey(apiKey).headers(new HashMap<>()).build(), opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, SkillVersion.class));
  }

  public CompletableFuture<JsonObject> downloadVersionAsync(String skillId, String version) {
    if (skillId == null || skillId.isEmpty() || version == null || version.isEmpty()) {
      return AsyncHelper.failedFuture(
          new InputRequiredException("skillId and version are required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.GET,
            StringUtils.format("skills/%s/versions/%s/content", skillId, version),
            baseUrl);
    return AsyncHelper.callAsync(
            api, GeneralGetParam.builder().apiKey(apiKey).headers(new HashMap<>()).build(), opt)
        .thenApply(
            result -> {
              Object output = result.getOutput();
              if (output instanceof JsonElement && ((JsonElement) output).isJsonObject()) {
                return ((JsonElement) output).getAsJsonObject();
              }
              return new JsonObject();
            });
  }

  @Override
  public void close() {
    if (files != null) {
      files.close();
    }
  }
}
