// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio;

import com.alibaba.dashscope.agentstudio.resource.Agents;
import com.alibaba.dashscope.agentstudio.resource.DeploymentRuns;
import com.alibaba.dashscope.agentstudio.resource.Deployments;
import com.alibaba.dashscope.agentstudio.resource.Environments;
import com.alibaba.dashscope.agentstudio.resource.Files;
import com.alibaba.dashscope.agentstudio.resource.Sessions;
import com.alibaba.dashscope.agentstudio.resource.Skills;
import com.alibaba.dashscope.agentstudio.resource.Vaults;
import com.alibaba.dashscope.protocol.ConnectionOptions;
import java.io.Closeable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class AgentStudioClient implements Closeable {
  private final Agents agents;
  private final Deployments deployments;
  private final DeploymentRuns deploymentRuns;
  private final Sessions sessions;
  private final Environments environments;
  private final Skills skills;
  private final Vaults vaults;
  private final Files files;
  private final String baseUrl;

  public AgentStudioClient() {
    this(null, null, null, null, null);
  }

  public AgentStudioClient(String workspace) {
    this(null, null, workspace, null, null);
  }

  public AgentStudioClient(String apiKey, String workspace) {
    this(apiKey, null, workspace, null, null);
  }

  public AgentStudioClient(
      String apiKey,
      String baseUrl,
      String workspace,
      String region,
      ConnectionOptions connectionOptions) {
    this.baseUrl = resolveBaseUrl(baseUrl, workspace, region);
    this.agents = new Agents(this.baseUrl, connectionOptions, apiKey);
    this.deployments = new Deployments(this.baseUrl, connectionOptions, apiKey);
    this.deploymentRuns = new DeploymentRuns(this.baseUrl, connectionOptions, apiKey);
    this.sessions = new Sessions(this.baseUrl, connectionOptions, apiKey);
    this.environments = new Environments(this.baseUrl, connectionOptions, apiKey);
    this.files = new Files(this.baseUrl, connectionOptions, apiKey);
    this.skills = new Skills(this.baseUrl, connectionOptions, apiKey, this.files);
    this.vaults = new Vaults(this.baseUrl, connectionOptions, apiKey);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String apiKey;
    private String baseUrl;
    private String workspace;
    private String region;
    private ConnectionOptions connectionOptions;

    public Builder apiKey(String apiKey) {
      this.apiKey = apiKey;
      return this;
    }

    public Builder baseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
      return this;
    }

    public Builder workspace(String workspace) {
      this.workspace = workspace;
      return this;
    }

    public Builder region(String region) {
      this.region = region;
      return this;
    }

    public Builder connectionOptions(ConnectionOptions connectionOptions) {
      this.connectionOptions = connectionOptions;
      return this;
    }

    public AgentStudioClient build() {
      return new AgentStudioClient(apiKey, baseUrl, workspace, region, connectionOptions);
    }
  }

  public Agents agents() {
    return agents;
  }

  public Deployments deployments() {
    return deployments;
  }

  public DeploymentRuns deploymentRuns() {
    return deploymentRuns;
  }

  public Sessions sessions() {
    return sessions;
  }

  public Environments environments() {
    return environments;
  }

  public Skills skills() {
    return skills;
  }

  public Vaults vaults() {
    return vaults;
  }

  public Files files() {
    return files;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public <T> CompletableFuture<T> async(Supplier<T> supplier) {
    return CompletableFuture.supplyAsync(supplier);
  }

  @Override
  public void close() {
    sessions.events().close();
    skills.close();
    files.close();
  }

  private static String resolveBaseUrl(String explicitUrl, String workspace, String region) {
    if (explicitUrl != null && !explicitUrl.isEmpty()) {
      return explicitUrl;
    }
    String envUrl = System.getenv(AgentStudioConstants.ENV_BASE_URL);
    if (envUrl == null || envUrl.isEmpty()) {
      envUrl = System.getenv(AgentStudioConstants.ENV_BASE_URL_ALT);
    }
    if (envUrl != null && !envUrl.isEmpty()) {
      return envUrl;
    }
    return AgentStudioConstants.resolveBaseUrl(workspace, region);
  }
}
