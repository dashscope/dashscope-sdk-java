package com.dashscope;

import com.dashscope.common.ResultCallback;
import com.dashscope.conversation.ConversationParamBase;
import com.dashscope.conversation.ConversationResultBase;
import io.reactivex.Flowable;

public interface BaseConversation {

  /**
   * Call the server to get the result in the callback function.
   *
   * @param param The input param, should be the subclass of `ConversationParamBase`.
   * @param callback The callback to receive response, should be the subclass of
   *     `ConversationResultBase`.
   */
  void call(ConversationParamBase param, ResultCallback<ConversationResultBase> callback);

  /**
   * Call the server to get the result by stream.
   *
   * @param param The input param, should be the subclass of `ConversationParamBase`.
   * @return A `Flowable` of the output structure, which is the subclass of
   *     `ConversationResultBase`.
   */
  Flowable<ConversationResultBase> streamCall(ConversationParamBase param);

  /**
   * Call the server to get the whole result.
   *
   * @param param The input param, should be the subclass of `ConversationParamBase`.
   * @return The output structure, should be the subclass of `ConversationResultBase`.
   */
  ConversationResultBase call(ConversationParamBase param);
}
