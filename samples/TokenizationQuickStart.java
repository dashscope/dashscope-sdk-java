// Copyright (c) Alibaba, Inc. and its affiliates.

import java.util.concurrent.Semaphore;
import com.alibaba.dashscope.aigc.generation.models.QwenParam;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.MessageManager;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.tokenizers.Tokenization;
import com.alibaba.dashscope.tokenizers.TokenizationResult;
import com.alibaba.dashscope.utils.JsonUtils;

public class TokenizationQuickStart {
  public static void qwenQuickStart()
      throws NoApiKeyException, ApiException, InputRequiredException {
    Tokenization tokenizer = new Tokenization();
    QwenParam param = QwenParam.builder()
    .model(Tokenization.Models.QWEN_TURBO)
    .prompt("如何做土豆炖猪脚?")
    .build();
    TokenizationResult result = tokenizer.call(param);
    System.out.println(JsonUtils.toJson(result));
  }

  public static void qwenQuickStartCallback()
      throws NoApiKeyException, ApiException, InputRequiredException, InterruptedException {
    Tokenization tokenizer = new Tokenization();
    QwenParam param = QwenParam.builder()
    .model(Tokenization.Models.QWEN_PLUS)
    .prompt("如何做土豆炖猪脚?")
    .build();
    Semaphore semaphore = new Semaphore(0);
    tokenizer.call(param, new ResultCallback<TokenizationResult>() {

      @Override
      public void onEvent(TokenizationResult message) {
        System.out.println(message);
      }

      @Override
      public void onError(Exception ex) {
        System.out.println(ex.getMessage());
        semaphore.release();
      }

      @Override
      public void onComplete() {
        System.out.println("onComplete");
        semaphore.release();
      }

    });
    semaphore.acquire();
  }

  public static void quickStartWithMessage() throws ApiException, NoApiKeyException, InputRequiredException {
    Tokenization tokenizer = new Tokenization();
    MessageManager messageManager = new MessageManager(10);
    messageManager.add(Message.builder().role(Role.USER.getValue()).content("今天天气好吗？").build());
    messageManager
        .add(Message.builder().role(Role.ASSISTANT.getValue()).content("今天天气不错，要出去玩玩嘛？").build());
    messageManager.add(Message.builder().role(Role.USER.getValue()).content("那你有什么地方推荐？").build());
    QwenParam param = QwenParam.builder()
    .model(Tokenization.Models.QWEN_PLUS)
    .messages(messageManager.get())
    .build();
    TokenizationResult result = tokenizer.call(param);
    System.out.println(result);
  }

  public static void main(String[] args) {
    try {
      qwenQuickStart();
    } catch (ApiException | NoApiKeyException | InputRequiredException e) {
      System.out.println(String.format("Exception %s", e.getMessage()));
    }
    System.exit(0);
  }
}
