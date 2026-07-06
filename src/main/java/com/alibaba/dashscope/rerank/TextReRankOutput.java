// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.rerank;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;

@Data
public class TextReRankOutput {

  @Data
  public static class Result {
    private Integer index;

    @SerializedName("relevance_score")
    private Double relevanceScore;

    private Document document;
  }

  @Data
  public static class Document {
    private String text;
  }

  private List<Result> results;
}
