// Copyright (c) Alibaba, Inc. and its affiliates.

import com.alibaba.dashscope.embeddings.MultiModalEmbedding;
import com.alibaba.dashscope.embeddings.MultiModalEmbeddingItemAudio;
import com.alibaba.dashscope.embeddings.MultiModalEmbeddingItemImage;
import com.alibaba.dashscope.embeddings.MultiModalEmbeddingItemText;
import com.alibaba.dashscope.embeddings.MultiModalEmbeddingParam;
import com.alibaba.dashscope.embeddings.MultiModalEmbeddingResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import java.util.Arrays;

public class MultiModalEmbeddingUsage {
  public static void basicCall() throws ApiException, NoApiKeyException, UploadFileException {
    MultiModalEmbedding embedding = new MultiModalEmbedding();
    MultiModalEmbeddingItemText text = MultiModalEmbeddingItemText.builder().text("冬雪").build();
    MultiModalEmbeddingItemImage image = new MultiModalEmbeddingItemImage(
        "https://modelscope.oss-cn-beijing.aliyuncs.com/resource/panda.jpeg");

    MultiModalEmbeddingItemAudio audio = new MultiModalEmbeddingItemAudio(
        "https://data-generator-idst.oss-cn-shanghai.aliyuncs.com/dashscope/image/multi_embedding/audio/cow.flac");

    MultiModalEmbeddingParam param = MultiModalEmbeddingParam.builder()
        .model(MultiModalEmbedding.Models.MULTIMODAL_EMBEDDING_ONE_PEACE_V1)
        .contents(Arrays.asList(audio, image, text)).build();
    MultiModalEmbeddingResult result = embedding.call(param);
    System.out.print(result);
  }

  public static void localFileCall() throws ApiException, NoApiKeyException, UploadFileException {
    MultiModalEmbedding embedding = new MultiModalEmbedding();
    MultiModalEmbeddingItemText text = MultiModalEmbeddingItemText.builder().text("冬雪").build();
    MultiModalEmbeddingItemImage image =
        new MultiModalEmbeddingItemImage("file://The_local_absolute_file_path");

    MultiModalEmbeddingParam param = MultiModalEmbeddingParam.builder()
        .model(MultiModalEmbedding.Models.MULTIMODAL_EMBEDDING_ONE_PEACE_V1)
        .contents(Arrays.asList(image, text)).build();
    MultiModalEmbeddingResult result = embedding.call(param);
    System.out.print(result);
  }

  public static void main(String[] args) {
    try {
      localFileCall();
      // basicCall();
    } catch (ApiException | NoApiKeyException | UploadFileException e) {
      System.out.println(e.getMessage());
    }
    System.exit(0);
  }
}
