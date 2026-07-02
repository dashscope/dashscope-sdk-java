// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.resource;

import com.alibaba.dashscope.agentstudio.AgentStudioConstants;
import com.alibaba.dashscope.agentstudio.model.AgentStudioDeletionStatus;
import com.alibaba.dashscope.agentstudio.model.Credential;
import com.alibaba.dashscope.agentstudio.model.Vault;
import com.alibaba.dashscope.agentstudio.pagination.CursorPage;
import com.alibaba.dashscope.agentstudio.param.CredentialCreateParam;
import com.alibaba.dashscope.agentstudio.param.CredentialListParam;
import com.alibaba.dashscope.agentstudio.param.CredentialUpdateParam;
import com.alibaba.dashscope.agentstudio.param.VaultCreateParam;
import com.alibaba.dashscope.agentstudio.param.VaultListParam;
import com.alibaba.dashscope.agentstudio.param.VaultUpdateParam;
import com.alibaba.dashscope.api.GeneralApi;
import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.common.FlattenResultBase;
import com.alibaba.dashscope.common.GeneralGetParam;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.protocol.ConnectionOptions;
import com.alibaba.dashscope.protocol.GeneralServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.utils.StringUtils;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class Vaults {
  private final GeneralApi<HalfDuplexParamBase> api;
  private final String baseUrl;
  private final String apiKey;
  private final Credentials credentials;

  public Vaults(String baseUrl, ConnectionOptions connectionOptions, String apiKey) {
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.api = connectionOptions != null ? new GeneralApi<>(connectionOptions) : new GeneralApi<>();
    this.credentials = new Credentials();
  }

  public Credentials credentials() {
    return credentials;
  }

  public Vault create(VaultCreateParam param) {
    return AsyncHelper.joinAndUnwrap(createAsync(param));
  }

  public Vault retrieve(String vaultId) {
    return retrieve(vaultId, null, null);
  }

  public Vault retrieve(String vaultId, String apiKey, Map<String, String> headers) {
    return AsyncHelper.joinAndUnwrap(retrieveAsync(vaultId, apiKey, headers));
  }

  public Vault update(String vaultId, VaultUpdateParam param) {
    return AsyncHelper.joinAndUnwrap(updateAsync(vaultId, param));
  }

  public CursorPage<Vault> list(VaultListParam param) {
    return AsyncHelper.joinAndUnwrap(listAsync(param));
  }

  public Vault archive(String vaultId) {
    return AsyncHelper.joinAndUnwrap(archiveAsync(vaultId));
  }

  public AgentStudioDeletionStatus delete(String vaultId) {
    return delete(vaultId, null, null);
  }

  public AgentStudioDeletionStatus delete(
      String vaultId, String apiKey, Map<String, String> headers) {
    return AsyncHelper.joinAndUnwrap(deleteAsync(vaultId, apiKey, headers));
  }

  public CompletableFuture<Vault> createAsync(VaultCreateParam param) {
    if (param == null) {
      return AsyncHelper.failedFuture(new InputRequiredException("param is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(HttpMethod.POST, "vaults", baseUrl);
    return AsyncHelper.callAsync(api, AgentStudioConstants.withApiKey(apiKey, param), opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Vault.class));
  }

  public CompletableFuture<Vault> retrieveAsync(String vaultId) {
    return retrieveAsync(vaultId, null, null);
  }

  public CompletableFuture<Vault> retrieveAsync(
      String vaultId, String apiKey, Map<String, String> headers) {
    if (vaultId == null || vaultId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("vaultId is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.GET, StringUtils.format("vaults/%s", vaultId), baseUrl);
    String resolvedKey = apiKey != null ? apiKey : this.apiKey;
    return AsyncHelper.callAsync(
            api,
            GeneralGetParam.builder()
                .apiKey(resolvedKey)
                .headers(headers != null ? headers : new HashMap<>())
                .build(),
            opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Vault.class));
  }

  public CompletableFuture<Vault> updateAsync(String vaultId, VaultUpdateParam param) {
    if (vaultId == null || vaultId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("vaultId is required!"));
    }
    if (param == null) {
      return AsyncHelper.failedFuture(new InputRequiredException("param is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.POST, StringUtils.format("vaults/%s", vaultId), baseUrl);
    return AsyncHelper.callAsync(api, AgentStudioConstants.withApiKey(apiKey, param), opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Vault.class));
  }

  public CompletableFuture<CursorPage<Vault>> listAsync(VaultListParam param) {
    if (param == null) {
      return AsyncHelper.failedFuture(new InputRequiredException("param is required!"));
    }
    String query = param.toQueryString();
    String path = query.isEmpty() ? "vaults" : "vaults?" + query;
    GeneralServiceOption opt = AgentStudioConstants.newServiceOption(HttpMethod.GET, path, baseUrl);
    return AsyncHelper.callAsync(
            api, GeneralGetParam.builder().apiKey(apiKey).headers(new HashMap<>()).build(), opt)
        .thenApply(
            r -> {
              Type type = new TypeToken<CursorPage<Vault>>() {}.getType();
              CursorPage<Vault> page = FlattenResultBase.fromDashScopeResult(r, type);
              page.setFetchNext(
                  cursor ->
                      listAsync(
                          VaultListParam.builder()
                              .limit(param.getLimit())
                              .includeArchived(param.getIncludeArchived())
                              .page(cursor)
                              .build()));
              return page;
            });
  }

  public CompletableFuture<Vault> archiveAsync(String vaultId) {
    if (vaultId == null || vaultId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("vaultId is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.POST, StringUtils.format("vaults/%s/archive", vaultId), baseUrl);
    return AsyncHelper.callAsync(
            api, AgentStudioConstants.withApiKey(apiKey, VaultUpdateParam.builder().build()), opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Vault.class));
  }

  public CompletableFuture<AgentStudioDeletionStatus> deleteAsync(String vaultId) {
    return deleteAsync(vaultId, null, null);
  }

  public CompletableFuture<AgentStudioDeletionStatus> deleteAsync(
      String vaultId, String apiKey, Map<String, String> headers) {
    if (vaultId == null || vaultId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("vaultId is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.DELETE, StringUtils.format("vaults/%s", vaultId), baseUrl);
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

  public final class Credentials {

    public Credential create(String vaultId, CredentialCreateParam param) {
      return AsyncHelper.joinAndUnwrap(createAsync(vaultId, param));
    }

    public Credential retrieve(String vaultId, String credentialId) {
      return retrieve(vaultId, credentialId, null, null);
    }

    public Credential retrieve(
        String vaultId, String credentialId, String apiKey, Map<String, String> headers) {
      return AsyncHelper.joinAndUnwrap(retrieveAsync(vaultId, credentialId, apiKey, headers));
    }

    public Credential update(String vaultId, String credentialId, CredentialUpdateParam param) {
      return AsyncHelper.joinAndUnwrap(updateAsync(vaultId, credentialId, param));
    }

    public CursorPage<Credential> list(String vaultId, CredentialListParam param) {
      return AsyncHelper.joinAndUnwrap(listAsync(vaultId, param));
    }

    public Credential archive(String vaultId, String credentialId) {
      return AsyncHelper.joinAndUnwrap(archiveAsync(vaultId, credentialId));
    }

    public AgentStudioDeletionStatus delete(String vaultId, String credentialId) {
      return delete(vaultId, credentialId, null, null);
    }

    public AgentStudioDeletionStatus delete(
        String vaultId, String credentialId, String apiKey, Map<String, String> headers) {
      return AsyncHelper.joinAndUnwrap(deleteAsync(vaultId, credentialId, apiKey, headers));
    }

    public CompletableFuture<Credential> createAsync(String vaultId, CredentialCreateParam param) {
      if (vaultId == null || vaultId.isEmpty()) {
        return AsyncHelper.failedFuture(new InputRequiredException("vaultId is required!"));
      }
      if (param == null) {
        return AsyncHelper.failedFuture(new InputRequiredException("param is required!"));
      }
      GeneralServiceOption opt =
          AgentStudioConstants.newServiceOption(
              HttpMethod.POST, StringUtils.format("vaults/%s/credentials", vaultId), baseUrl);
      return AsyncHelper.callAsync(api, AgentStudioConstants.withApiKey(apiKey, param), opt)
          .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Credential.class));
    }

    public CompletableFuture<Credential> retrieveAsync(String vaultId, String credentialId) {
      return retrieveAsync(vaultId, credentialId, null, null);
    }

    public CompletableFuture<Credential> retrieveAsync(
        String vaultId, String credentialId, String apiKey, Map<String, String> headers) {
      if (vaultId == null || vaultId.isEmpty() || credentialId == null || credentialId.isEmpty()) {
        return AsyncHelper.failedFuture(
            new InputRequiredException("vaultId and credentialId are required!"));
      }
      GeneralServiceOption opt =
          AgentStudioConstants.newServiceOption(
              HttpMethod.GET,
              StringUtils.format("vaults/%s/credentials/%s", vaultId, credentialId),
              baseUrl);
      String resolvedKey = apiKey != null ? apiKey : Vaults.this.apiKey;
      return AsyncHelper.callAsync(
              api,
              GeneralGetParam.builder()
                  .apiKey(resolvedKey)
                  .headers(headers != null ? headers : new HashMap<>())
                  .build(),
              opt)
          .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Credential.class));
    }

    public CompletableFuture<Credential> updateAsync(
        String vaultId, String credentialId, CredentialUpdateParam param) {
      if (vaultId == null || vaultId.isEmpty() || credentialId == null || credentialId.isEmpty()) {
        return AsyncHelper.failedFuture(
            new InputRequiredException("vaultId and credentialId are required!"));
      }
      if (param == null) {
        return AsyncHelper.failedFuture(new InputRequiredException("param is required!"));
      }
      GeneralServiceOption opt =
          AgentStudioConstants.newServiceOption(
              HttpMethod.POST,
              StringUtils.format("vaults/%s/credentials/%s", vaultId, credentialId),
              baseUrl);
      return AsyncHelper.callAsync(api, AgentStudioConstants.withApiKey(apiKey, param), opt)
          .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Credential.class));
    }

    public CompletableFuture<CursorPage<Credential>> listAsync(
        String vaultId, CredentialListParam param) {
      if (vaultId == null || vaultId.isEmpty()) {
        return AsyncHelper.failedFuture(new InputRequiredException("vaultId is required!"));
      }
      if (param == null) {
        return AsyncHelper.failedFuture(new InputRequiredException("param is required!"));
      }
      String query = param.toQueryString();
      String path = StringUtils.format("vaults/%s/credentials", vaultId);
      if (!query.isEmpty()) {
        path = path + "?" + query;
      }
      GeneralServiceOption opt =
          AgentStudioConstants.newServiceOption(HttpMethod.GET, path, baseUrl);
      return AsyncHelper.callAsync(
              api, GeneralGetParam.builder().apiKey(apiKey).headers(new HashMap<>()).build(), opt)
          .thenApply(
              r -> {
                Type type = new TypeToken<CursorPage<Credential>>() {}.getType();
                CursorPage<Credential> page = FlattenResultBase.fromDashScopeResult(r, type);
                page.setFetchNext(
                    cursor ->
                        listAsync(
                            vaultId,
                            CredentialListParam.builder()
                                .limit(param.getLimit())
                                .includeArchived(param.getIncludeArchived())
                                .page(cursor)
                                .build()));
                return page;
              });
    }

    public CompletableFuture<Credential> archiveAsync(String vaultId, String credentialId) {
      if (vaultId == null || vaultId.isEmpty() || credentialId == null || credentialId.isEmpty()) {
        return AsyncHelper.failedFuture(
            new InputRequiredException("vaultId and credentialId are required!"));
      }
      GeneralServiceOption opt =
          AgentStudioConstants.newServiceOption(
              HttpMethod.POST,
              StringUtils.format("vaults/%s/credentials/%s/archive", vaultId, credentialId),
              baseUrl);
      return AsyncHelper.callAsync(
              api,
              AgentStudioConstants.withApiKey(apiKey, CredentialUpdateParam.builder().build()),
              opt)
          .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Credential.class));
    }

    public CompletableFuture<AgentStudioDeletionStatus> deleteAsync(
        String vaultId, String credentialId) {
      return deleteAsync(vaultId, credentialId, null, null);
    }

    public CompletableFuture<AgentStudioDeletionStatus> deleteAsync(
        String vaultId, String credentialId, String apiKey, Map<String, String> headers) {
      if (vaultId == null || vaultId.isEmpty() || credentialId == null || credentialId.isEmpty()) {
        return AsyncHelper.failedFuture(
            new InputRequiredException("vaultId and credentialId are required!"));
      }
      GeneralServiceOption opt =
          AgentStudioConstants.newServiceOption(
              HttpMethod.DELETE,
              StringUtils.format("vaults/%s/credentials/%s", vaultId, credentialId),
              baseUrl);
      String resolvedKey = apiKey != null ? apiKey : Vaults.this.apiKey;
      return AsyncHelper.callAsync(
              api,
              GeneralGetParam.builder()
                  .apiKey(resolvedKey)
                  .headers(headers != null ? headers : new HashMap<>())
                  .build(),
              opt)
          .thenApply(
              r -> FlattenResultBase.fromDashScopeResult(r, AgentStudioDeletionStatus.class));
    }
  }
}
