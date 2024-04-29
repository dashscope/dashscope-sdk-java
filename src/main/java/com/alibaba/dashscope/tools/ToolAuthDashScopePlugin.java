package com.alibaba.dashscope.tools;

import com.google.gson.annotations.SerializedName;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.SuperBuilder;

/** DashScope auth: "auth": { "type": "dashscope_plugin", "user_token": "the api-key value" } */
@Data
@SuperBuilder
public class ToolAuthDashScopePlugin implements ToolAuthenticationBase {
  @Default String type = "dashscope_plugin";

  @SerializedName("user_token")
  String userToken;
}
