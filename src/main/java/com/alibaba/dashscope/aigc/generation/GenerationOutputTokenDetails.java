package com.alibaba.dashscope.aigc.generation;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class GenerationOutputTokenDetails {
    @SerializedName("reasoning_tokens")
    private Integer reasoningTokens;
}
