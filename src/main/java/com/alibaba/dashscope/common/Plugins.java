// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.common;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class Plugins {

  @Data
  public static class Search {
    @SerializedName("count")
    private Integer count;

    @SerializedName("strategy")
    private String strategy;
  }

  @SerializedName("search")
  private Search search;
}
