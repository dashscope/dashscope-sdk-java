// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.conversation.rpc;

import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.conversation.ConversationParam;
import com.alibaba.dashscope.conversation.ConversationResult;

public interface ConversationRpc {

  /**
   * Call the Conversation rpc protocol to communicate with the server side.
   *
   * @param param The input param of class `ConversationParam`.
   * @param callback The callback to receive response, the template class is `ConversationResult`.
   */
  void call(ConversationParam param, ResultCallback<ConversationResult> callback);
}
