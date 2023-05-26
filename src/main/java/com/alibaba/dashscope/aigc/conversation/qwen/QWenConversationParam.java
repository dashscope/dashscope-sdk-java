// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.aigc.conversation.qwen;

import static com.alibaba.dashscope.utils.ApiKeywords.*;

import com.alibaba.dashscope.aigc.conversation.ConversationParam;
import com.alibaba.dashscope.common.Result;
import com.google.gson.JsonObject;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
@SuperBuilder
public class QWenConversationParam extends ConversationParam {

  public static final String QWEN_V1 = "qwen-v1";

  /** The max_length parameter for the generation algo. */
  private Integer maxLength;

  /** The top_p parameter for the generation algo. */
  private Double topP;

  /** Enable search. */
  @Builder.Default private boolean enableSearch = false;

  /** Multi-modal conversation. */
  @Builder.Default private boolean multiModal = false;

  /** Enable debug or not. */
  @Builder.Default private boolean enableDebug = false;

  protected JsonObject getParameters() {
    JsonObject json = new JsonObject();
    if (topP != null) {
      json.addProperty(TOP_P, topP);
    }
    if (maxLength != null) {
      json.addProperty(MAX_LENGTH, maxLength);
    }
    json.addProperty(M6_SEARCH_IN_FIRST_QUERY, 0);
    json.addProperty(M6_NUM_WEB_SEARCH, enableSearch ? 5 : 0);
    return json;
  }

  @Override
  public Class<? extends Result> resultType() {
    return QWenConversationResult.class;
  }
}
