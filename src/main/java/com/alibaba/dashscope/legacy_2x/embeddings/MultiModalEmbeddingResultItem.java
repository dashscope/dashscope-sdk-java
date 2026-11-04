// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.legacy_2x.embeddings;

import java.util.List;
import lombok.Data;

@Data
@Deprecated
public class MultiModalEmbeddingResultItem {
  private Integer index;

  private String type;

  private List<Double> embedding;
}
