// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.resource;

import com.alibaba.dashscope.agentstudio.AgentStudioConstants;
import com.alibaba.dashscope.agentstudio.param.SecurityListAgentLogsParam;
import com.alibaba.dashscope.api.GeneralApi;
import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.common.FlattenResultBase;
import com.alibaba.dashscope.common.GeneralGetParam;
import com.alibaba.dashscope.protocol.ConnectionOptions;
import com.alibaba.dashscope.protocol.GeneralServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.Data;
import lombok.EqualsAndHashCode;

public final class Security {
  private final GeneralApi<HalfDuplexParamBase> api;
  private final String baseUrl;
  private final String apiKey;

  public Security(String baseUrl, ConnectionOptions connectionOptions, String apiKey) {
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.api = connectionOptions != null ? new GeneralApi<>(connectionOptions) : new GeneralApi<>();
  }

  // ── sync ──

  public SecurityOverview overview() {
    return AsyncHelper.joinAndUnwrap(overviewAsync());
  }

  public SecurityAlertList listAgentLogs(SecurityListAgentLogsParam param) {
    return AsyncHelper.joinAndUnwrap(listAgentLogsAsync(param));
  }

  // ── async ──

  public CompletableFuture<SecurityOverview> overviewAsync() {
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(HttpMethod.GET, "security/overview", baseUrl);
    return AsyncHelper.callAsync(
            api, GeneralGetParam.builder().apiKey(apiKey).headers(new HashMap<>()).build(), opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, SecurityOverview.class));
  }

  public CompletableFuture<SecurityAlertList> listAgentLogsAsync(SecurityListAgentLogsParam param) {
    if (param == null) {
      param = SecurityListAgentLogsParam.builder().build();
    }
    String query = param.toQueryString();
    String path = query.isEmpty() ? "security/agent_logs" : "security/agent_logs?" + query;
    GeneralServiceOption opt = AgentStudioConstants.newServiceOption(HttpMethod.GET, path, baseUrl);
    return AsyncHelper.callAsync(
            api, GeneralGetParam.builder().apiKey(apiKey).headers(new HashMap<>()).build(), opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, SecurityAlertList.class));
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class SecurityOverview extends FlattenResultBase {
    @SerializedName("capabilities")
    private List<SecurityOverview.SecurityCapability> capabilities;

    @SerializedName("protection")
    private List<SecurityOverview.SecurityCapability> protection;

    @SerializedName("content_safety")
    private SecurityOverview.SecurityScanStat contentSafety;

    @SerializedName("file_scan")
    private SecurityOverview.SecurityScanStat fileScan;

    @SerializedName("skill_scan")
    private SecurityOverview.SecurityScanStat skillScan;

    @Data
    public static class SecurityCapability {
      @SerializedName("key")
      private String key;

      @SerializedName("enabled")
      private Boolean enabled;
    }

    @Data
    public static class SecurityScanStat {
      @SerializedName("hit")
      private Long hit;

      @SerializedName("scanned")
      private Long scanned;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class SecurityAlertList extends FlattenResultBase {
    @SerializedName("stats")
    private SecurityAlertList.SecurityAlertStats stats;

    @SerializedName("data")
    private List<SecurityAlertList.SecurityAlert> data;

    @SerializedName("next_page")
    private String nextPage;

    @Data
    public static class SecurityAlertStats {
      @SerializedName("total")
      private Long total;

      @SerializedName("high")
      private Long high;

      @SerializedName("medium")
      private Long medium;

      @SerializedName("low")
      private Long low;
    }

    @Data
    public static class SecurityAlert {
      @SerializedName("alert_id")
      private String alertId;

      @SerializedName("risk_level")
      private String riskLevel;

      @SerializedName("risk_name")
      private String riskName;

      @SerializedName("risk_desc")
      private String riskDesc;

      @SerializedName("asset_type")
      private String assetType;

      @SerializedName("asset_name")
      private String assetName;

      @SerializedName("app_id")
      private String appId;

      @SerializedName("app_name")
      private String appName;

      @SerializedName("agent_name")
      private String agentName;

      @SerializedName("status")
      private String status;

      @SerializedName("source")
      private String source;

      @SerializedName("check_time")
      private String checkTime;

      @SerializedName("handle_time")
      private String handleTime;

      @SerializedName("vendor")
      private String vendor;
    }
  }
}
