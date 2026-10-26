// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.tokenization;

import com.alibaba.dashscope.types.common.ResponseBase;
import com.alibaba.dashscope.types.common.Usage;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Server-side token count (Python v2 {@code TokenCount} parity): usage at the envelope top, the
 * tokenizer's ids/tokens lifted out of {@code output}.
 */
public class TokenCount extends ResponseBase {
  @JsonProperty("usage")
  private Usage usage;

  @JsonProperty("token_ids")
  private List<Integer> tokenIds;

  @JsonProperty("tokens")
  private List<String> tokens;

  public Usage getUsage() {
    return usage;
  }

  public List<Integer> getTokenIds() {
    return tokenIds;
  }

  public List<String> getTokens() {
    return tokens;
  }

  /** Convenience for {@code usage.input_tokens}; 0 when usage is absent. */
  public int getInputTokens() {
    return usage == null ? 0 : usage.getInputTokens();
  }
}
