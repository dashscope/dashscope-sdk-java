package com.alibaba.dashscope.embeddings;

import java.util.List;
import lombok.Data;

@Data
public final class TextEmbeddingOutput {
  private List<TextEmbeddingResultItem> embeddings;
}
