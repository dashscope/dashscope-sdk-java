// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.model;

import com.alibaba.dashscope.common.FlattenResultBase;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Non-paginated list of webhook endpoints in the current workspace. */
@Data
@EqualsAndHashCode(callSuper = true)
public class WebhookEndpointList extends FlattenResultBase {
  @SerializedName("data")
  private List<WebhookEndpoint> data;
}
