package com.alibaba.dashscope.legacy_2x.embeddings;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;

@Data
@Deprecated
public class TextEmbeddingResultItem {
  @SerializedName("text_index")
  private Integer textIndex;

  private List<Double> embedding;

  @SerializedName("sparse_embedding")
  private List<TextEmbeddingSparseEmbedding> sparseEmbedding;
}
