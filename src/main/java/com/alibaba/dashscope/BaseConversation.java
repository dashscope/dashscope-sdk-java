// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import com.alibaba.dashscope.aigc.conversation.ConversationParam;
import com.alibaba.dashscope.aigc.conversation.ConversationResult;
import com.alibaba.dashscope.common.ResultCallback;
import io.reactivex.Flowable;

public interface BaseConversation {

  /**
   * Call the server to get the result in the callback function.
   *
   * @param param The input param, should be the subclass of `ConversationParam`.
   * @param callback The callback to receive response, should be the subclass of
   *     `ConversationResult`.
   */
  void call(ConversationParam param, ResultCallback<ConversationResult> callback);

  /**
   * Call the server to get the result by stream.
   *
   * @param param The input param, should be the subclass of `ConversationParam`.
   * @return A `Flowable` of the output structure, which is the subclass of `ConversationResult`.
   */
  Flowable<ConversationResult> streamCall(ConversationParam param);

  /**
   * Call the server to get the whole result.
   *
   * @param param The input param, should be the subclass of `ConversationParam`.
   * @return The output structure, should be the subclass of `ConversationResult`.
   */
  ConversationResult call(ConversationParam param);
}
