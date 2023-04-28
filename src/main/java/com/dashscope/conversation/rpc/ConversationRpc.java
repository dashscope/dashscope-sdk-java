package com.dashscope.conversation.rpc;

import com.dashscope.common.ResultCallback;
import com.dashscope.conversation.ConversationParamBase;
import com.dashscope.conversation.ConversationResultBase;

public interface ConversationRpc {

  /**
   * Call the Conversation rpc protocol to communicate with the server side.
   *
   * @param param The input param of class `ConversationParam`.
   * @param callback The callback to receive response, the template class is
   *     `ConversationResultBase`.
   */
  void call(ConversationParamBase param, ResultCallback<ConversationResultBase> callback);
}
