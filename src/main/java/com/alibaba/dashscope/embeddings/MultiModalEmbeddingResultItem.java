// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.embeddings;

import lombok.Data;
import java.util.List;

@Data
public class MultiModalEmbeddingResultItem {
    private Integer index;

    private String type;

    private List<Double> embedding;
}
