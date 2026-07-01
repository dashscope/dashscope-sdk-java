// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.model;

import com.alibaba.dashscope.common.FlattenResultBase;
import com.google.gson.annotations.SerializedName;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Credential extends FlattenResultBase {
  @SerializedName("id")
  private String id;

  @SerializedName("type")
  private String type;

  @SerializedName("vault_id")
  private String vaultId;

  @SerializedName("display_name")
  private String displayName;

  @SerializedName("auth")
  private CredentialAuth auth;

  @SerializedName("metadata")
  private Map<String, Object> metadata;

  @SerializedName("archived_at")
  private String archivedAt;

  @SerializedName("created_at")
  private String createdAt;

  @SerializedName("updated_at")
  private String updatedAt;

  @Data
  public static class Networking {
    @SerializedName("type")
    private String type;
  }

  @Data
  public static class CredentialAuth {
    @SerializedName("type")
    private String type;

    @SerializedName("token")
    private String token;

    @SerializedName("secret_name")
    private String secretName;

    @SerializedName("secret_value")
    private String secretValue;

    @SerializedName("mcp_server_url")
    private String mcpServerUrl;

    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("expires_at")
    private String expiresAt;

    @SerializedName("refresh")
    private String refresh;

    @SerializedName("networking")
    private Networking networking;
  }
}
