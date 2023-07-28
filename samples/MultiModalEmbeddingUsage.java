// Copyright (c) Alibaba, Inc. and its affiliates.

import com.alibaba.dashscope.embeddings.MultiModalEmbedding;
import com.alibaba.dashscope.embeddings.MultiModalEmbeddingItemAudio;
import com.alibaba.dashscope.embeddings.MultiModalEmbeddingItemImage;
import com.alibaba.dashscope.embeddings.MultiModalEmbeddingItemText;
import com.alibaba.dashscope.embeddings.MultiModalEmbeddingParam;
import com.alibaba.dashscope.embeddings.MultiModalEmbeddingResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import java.util.Arrays;

public class MultiModalEmbeddingUsage {
  public static void basicCall() throws ApiException, NoApiKeyException {
    MultiModalEmbedding embedding = new MultiModalEmbedding();
    MultiModalEmbeddingItemText text =
        MultiModalEmbeddingItemText.builder()
            .text("冬雪")
            .build();
    MultiModalEmbeddingItemImage image =
        new MultiModalEmbeddingItemImage(
            "https://modelscope.oss-cn-beijing.aliyuncs.com/resource/panda.jpeg");

    MultiModalEmbeddingItemAudio audio = new MultiModalEmbeddingItemAudio("https://modelscope.oss-cn-beijing.aliyuncs.com/resource/panda.jpeg");
    
    MultiModalEmbeddingParam param =
        MultiModalEmbeddingParam.builder()
            .model("pre-multimodal-embedding")
            .contents(Arrays.asList(audio, image, text))
            .build();
    MultiModalEmbeddingResult result = embedding.call(param);
    System.out.print(result);
  }

  public static void main(String[] args){
      try {
        basicCall();
      } catch (ApiException | NoApiKeyException e) {
        System.out.println(e.getMessage());
      }
      System.exit(0);
  }
}
