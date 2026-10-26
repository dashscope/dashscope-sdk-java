package com.alibaba.dashscope.legacy_2x.embeddings;

import java.util.List;
import lombok.Data;

@Data
public final class TextEmbeddingOutput {
  private List<TextEmbeddingResultItem> embeddings;
}
