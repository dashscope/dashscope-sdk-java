// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.param;

import com.alibaba.dashscope.base.FlattenHalfDuplexParamBase;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.util.Map;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class EnvironmentCreateParam extends FlattenHalfDuplexParamBase {
  @NonNull private String name;

  /**
   * Environment runtime configuration (open structure, aligned with server-side {@code
   * JSONObject}).
   *
   * <p>Known keys:
   *
   * <ul>
   *   <li>{@code type} (String) — hosting type, e.g. "cloud"
   *   <li>{@code networking} (Map) — e.g. {"type": "unrestricted"}
   *   <li>{@code packages} (Map&lt;String, List&lt;String&gt;&gt;) — e.g. {"pip": ["numpy"]}
   * </ul>
   *
   * <p>Example:
   *
   * <pre>{@code
   * Map<String, Object> config = new HashMap<>();
   * config.put("type", "cloud");
   * config.put("networking", Collections.singletonMap("type", "unrestricted"));
   *
   * Map<String, List<String>> packages = new HashMap<>();
   * packages.put("pip", Arrays.asList("numpy", "pandas"));
   * config.put("packages", packages);
   * }</pre>
   */
  @NonNull private Map<String, Object> config;

  @Default private String description = null;
  @Default private String scope = null;
  @Default private Map<String, String> metadata = null;

  @Override
  public JsonObject getHttpBody() {
    JsonObject body = new JsonObject();
    body.addProperty("name", name);
    body.add("config", JsonUtils.toJsonElement(config));
    if (description != null) {
      body.addProperty("description", description);
    }
    if (scope != null) {
      body.addProperty("scope", scope);
    }
    if (metadata != null && !metadata.isEmpty()) {
      body.add("metadata", JsonUtils.toJsonElement(metadata));
    }
    addExtraBody(body);
    return body;
  }
}
