package com.alibaba.dashscope.tokenizers;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;

@Data
public class TokenizationOutput {
  private String prompt;

  @SerializedName("token_ids")
  private List<Integer> tokenIds;

  private List<String> tokens;
}
