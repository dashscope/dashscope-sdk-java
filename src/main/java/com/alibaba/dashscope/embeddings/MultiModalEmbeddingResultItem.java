// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.embeddings;

import java.util.List;
import lombok.Data;

@Data
public class MultiModalEmbeddingResultItem {
  private Integer index;

  private String type;

  private List<Double> embedding;
}
