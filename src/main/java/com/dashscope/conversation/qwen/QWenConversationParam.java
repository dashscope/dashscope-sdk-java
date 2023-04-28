package com.dashscope.conversation.qwen;

import static com.dashscope.utils.ApiKeywords.*;

import com.alibaba.fastjson.JSONObject;
import com.dashscope.common.ErrorType;
import com.dashscope.common.Protocol;
import com.dashscope.conversation.ConversationMessage;
import com.dashscope.conversation.ConversationMessageStatus;
import com.dashscope.conversation.ConversationParamBase;
import com.dashscope.conversation.ConversationResultBase;
import com.dashscope.exception.ApiException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class QWenConversationParam extends ConversationParamBase {

  public static final String QWEN_V1 = "qwen-v1";

  /** The max_length parameter for the generation algo. */
  private Integer maxLength;

  /** The top_p parameter for the generation algo. */
  private Double topP;

  /** Enable search. */
  private boolean enableSearch = false;

  /** Multi-modal conversation. */
  private boolean multiModal = false;

  /** Enable debug or not. */
  private boolean enableDebug = false;

  /**
   * The n-round history to use, if > 0, the conversation api will choose 2 * nHistory ChatMessage
   * to pass to the server side.
   */
  private Integer nHistory;

  public JSONObject getParameters() {
    JSONObject json = new JSONObject();
    if (topP != null) {
      json.put(TOP_P, topP);
    }
    if (maxLength != null) {
      json.put(MAX_LENGTH, maxLength);
    }
    json.put(M6_SEARCH_IN_FIRST_QUERY, 0);
    json.put(M6_NUM_WEB_SEARCH, enableSearch ? 5 : 0);
    return json;
  }

  @Override
  public String url() {
    return "/services/aigc/text-generation/generation";
  }

  @Override
  public String buildMessageBody(String protocol) {
    ConversationMessage inputMessage =
        ConversationMessage.buildInputMessageWithHistory(
            this.getMsgId(),
            this.getModel(),
            this.getPrompt(),
            this.getHistory(),
            this.getNHistory(),
            this.getParameters(),
            this.isStream(),
            this.isEnableDebug());
    if (this.isMultiModal()) {
      inputMessage =
          ConversationMessage.buildMultiModalInputMessageWithHistory(
              this.getMsgId(),
              this.getModel(),
              this.getPrompt(),
              this.getHistory(),
              this.getNHistory(),
              this.getParameters(),
              this.isStream(),
              this.isEnableDebug());
    }
    if (Protocol.HTTP.getValue().equals(protocol)) {
      JSONObject json = new JSONObject();
      json.put(MODEL, inputMessage.getPayload().getModel());
      json.put(PARAMETERS, inputMessage.getPayload().getParameters());
      json.put(INPUT, inputMessage.getPayload().getInput());
      return json.toJSONString();
    } else if (Protocol.WEBSOCKET.getValue().equals(protocol)) {
      return JSONObject.toJSONString(inputMessage);
    } else {
      throw new ApiException(
          ConversationMessageStatus.builder()
              .code(ErrorType.PROTOCOL_UNSUPPORTED.getValue())
              .message(protocol)
              .build());
    }
  }

  @Override
  public Class<? extends ConversationResultBase> resultType() {
    return QWenConversationResult.class;
  }
}
