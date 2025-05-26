package com.alibaba.dashscope.embeddings;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;

@Data
public class TextEmbeddingResultItem {
  @SerializedName("text_index")
  private Integer textIndex;

  private List<Double> embedding;

  @SerializedName("sparse_embedding")
  private List<TextEmbeddingSparseEmbedding> sparseEmbedding;
}
