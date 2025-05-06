package com.alibaba.dashscope.aigc.generation;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public final class GenerationLogprobs {
   @Data
   public static class TopLogprob {
      private Double logprob;

      private List<Integer> bytes;

      private String token;
   }

   @Data
   public static class Content {
     @SerializedName("top_logprobs")
     private List<TopLogprob> topLogprobs;

     private Double logprob;

     private List<Integer> bytes;

     private String token;
   }

   private List<Content> content;
}


