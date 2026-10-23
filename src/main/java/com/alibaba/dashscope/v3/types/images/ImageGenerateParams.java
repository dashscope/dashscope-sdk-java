// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.images;

import java.util.List;
import java.util.Map;

/**
 * Parameters for {@code images().generate} / {@code images().jobs().create} (design §3). Two wire
 * protocols hide behind this one params object, picked by model name prefix ({@link
 * #MULTIMODAL_MODEL_PREFIXES}): legacy text2image takes {@code prompt} input and {@code
 * negative_prompt} in input; the multimodal-generation protocol takes a {@code messages} input and
 * {@code negative_prompt} in parameters. {@code size} accepts both {@code 1024x1024} and the wire
 * form {@code 1024*1024}.
 */
public final class ImageGenerateParams {
  /**
   * Models that only answer on the multimodal-generation protocol (prefix match, so {@code
   * -preview}-style variants route the same way).
   */
  public static final String[] MULTIMODAL_MODEL_PREFIXES = {"wan2.6-t2i", "wan2.6-image"};

  private final String model;
  private final String prompt;
  private final String negativePrompt;
  private final Integer n;
  private final String size;
  private final List<String> imageUrls;
  private final Map<String, Object> extraInput;
  private final String function;
  private final Integer seed;
  private final String style;
  private final Boolean promptExtend;
  private final Boolean watermark;
  private final Map<String, Object> parameters;

  private ImageGenerateParams(Builder b) {
    this.model = b.model;
    this.prompt = b.prompt;
    this.negativePrompt = b.negativePrompt;
    this.n = b.n;
    this.size = b.size;
    this.imageUrls = b.imageUrls;
    this.extraInput = b.extraInput;
    this.function = b.function;
    this.seed = b.seed;
    this.style = b.style;
    this.promptExtend = b.promptExtend;
    this.watermark = b.watermark;
    this.parameters = b.parameters;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** true when {@code model} routes to the multimodal-generation protocol. */
  public static boolean usesMultimodalProtocol(String model) {
    if (model == null) {
      return false;
    }
    for (String prefix : MULTIMODAL_MODEL_PREFIXES) {
      if (model.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  public String getModel() {
    return model;
  }

  public String getPrompt() {
    return prompt;
  }

  public String getNegativePrompt() {
    return negativePrompt;
  }

  public Integer getN() {
    return n;
  }

  public String getSize() {
    return size;
  }

  /**
   * Reference images (multimodal-protocol models); local paths and {@code file://}/{@code oss://}
   * values are uploaded/resolved before the call.
   */
  public List<String> getImageUrls() {
    return imageUrls;
  }

  /** Extra input fields merged into {@code input} verbatim. */
  public Map<String, Object> getExtraInput() {
    return extraInput;
  }

  public String getFunction() {
    return function;
  }

  public Integer getSeed() {
    return seed;
  }

  public String getStyle() {
    return style;
  }

  public Boolean getPromptExtend() {
    return promptExtend;
  }

  public Boolean getWatermark() {
    return watermark;
  }

  /** Passthrough for parameters this signature does not name; named fields win on conflict. */
  public Map<String, Object> getParameters() {
    return parameters;
  }

  public static final class Builder {
    private String model;
    private String prompt;
    private String negativePrompt;
    private Integer n;
    private String size;
    private List<String> imageUrls;
    private Map<String, Object> extraInput;
    private String function;
    private Integer seed;
    private String style;
    private Boolean promptExtend;
    private Boolean watermark;
    private Map<String, Object> parameters;

    private Builder() {}

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder prompt(String prompt) {
      this.prompt = prompt;
      return this;
    }

    public Builder negativePrompt(String negativePrompt) {
      this.negativePrompt = negativePrompt;
      return this;
    }

    public Builder n(Integer n) {
      this.n = n;
      return this;
    }

    public Builder size(String size) {
      this.size = size;
      return this;
    }

    public Builder imageUrls(List<String> imageUrls) {
      this.imageUrls = imageUrls;
      return this;
    }

    public Builder extraInput(Map<String, Object> extraInput) {
      this.extraInput = extraInput;
      return this;
    }

    public Builder function(String function) {
      this.function = function;
      return this;
    }

    public Builder seed(Integer seed) {
      this.seed = seed;
      return this;
    }

    public Builder style(String style) {
      this.style = style;
      return this;
    }

    public Builder promptExtend(Boolean promptExtend) {
      this.promptExtend = promptExtend;
      return this;
    }

    public Builder watermark(Boolean watermark) {
      this.watermark = watermark;
      return this;
    }

    public Builder parameters(Map<String, Object> parameters) {
      this.parameters = parameters;
      return this;
    }

    public ImageGenerateParams build() {
      if (model == null || model.isEmpty()) {
        throw new IllegalArgumentException("model is required");
      }
      if (prompt == null || prompt.isEmpty()) {
        throw new IllegalArgumentException("prompt is required");
      }
      return new ImageGenerateParams(this);
    }
  }
}
