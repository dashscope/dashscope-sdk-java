// Copyright (c) Alibaba, Inc. and its affiliates.

import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.rerank.TextReRank;
import com.alibaba.dashscope.rerank.TextReRankParam;
import com.alibaba.dashscope.rerank.TextReRankResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.JsonUtils;

import java.util.Arrays;

public class TextReRankTest {

  private static final String MODEL_NAME = System.getenv("MODEL_NAME");

  public static void main(String[] args) {
    try {
      // Create TextReRank instance
      TextReRank textReRank = new TextReRank();

      // Create parameters
      TextReRankParam param = TextReRankParam.builder()
          .model(MODEL_NAME)
          .query("什么是文本排序模型")
          .documents(Arrays.asList(
              "文本排序模型广泛用于搜索引擎和推荐系统中，它们根据文本相关性对候选文本进行排序",
              "量子计算是计算科学的一个前沿领域",
              "预训练语言模型的发展给文本排序模型带来了新的进展"
          ))
          .topN(10)
          .returnDocuments(true)
          .instruct("Retrieval document that can answer users query.")
          .build();

      // Call the API
      TextReRankResult result = textReRank.call(param);

      System.out.println("Rerank Result:");
      System.out.println(JsonUtils.toJson(result));
    } catch (NoApiKeyException e) {
      System.err.println("API key not found: " + e.getMessage());
    } catch (ApiException e) {
      System.err.println("API call failed: " + e.getMessage());
    } catch (InputRequiredException e) {
        throw new RuntimeException(e);
    }
  }
}
