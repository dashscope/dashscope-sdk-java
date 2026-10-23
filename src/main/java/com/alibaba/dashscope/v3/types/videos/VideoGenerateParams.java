// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.videos;

import java.util.List;
import java.util.Map;

/**
 * Parameters for {@code videos().generate} / {@code videos().jobs().create} (design §3). One
 * service, two input protocols, picked by model name prefix ({@link #MEDIA_MODEL_PREFIXES}): legacy
 * models take {@code img_url} first-frame input (the public name is {@code imageUrl}; {@code
 * prompt}-only works too); media-protocol models take a {@code media} list of typed assets ({@code
 * first_frame} / {@code last_frame} / {@code driving_audio} / {@code first_clip}), with {@code
 * imageUrl} accepted as shorthand for a lone {@code first_frame} asset.
 *
 * <p>Media items are wire-shaped maps ({@code {type, url}} plus any per-model extras); local paths
 * and {@code file://}/{@code oss://} values anywhere in the inputs are uploaded/resolved before the
 * call.
 */
public final class VideoGenerateParams {
  /**
   * Models that only answer on the media-based input protocol (prefix match, so dated variants
   * route the same way).
   */
  public static final String[] MEDIA_MODEL_PREFIXES = {"wan2.7-i2v"};

  private final String model;
  private final String prompt;
  private final String imageUrl;
  private final List<Map<String, Object>> media;
  private final Map<String, Object> extraInput;
  private final String function;
  private final Integer seed;
  private final String size;
  private final Integer duration;
  private final String resolution;
  private final String ratio;
  private final Boolean promptExtend;
  private final Boolean watermark;
  private final Map<String, Object> parameters;

  private VideoGenerateParams(Builder b) {
    this.model = b.model;
    this.prompt = b.prompt;
    this.imageUrl = b.imageUrl;
    this.media = b.media;
    this.extraInput = b.extraInput;
    this.function = b.function;
    this.seed = b.seed;
    this.size = b.size;
    this.duration = b.duration;
    this.resolution = b.resolution;
    this.ratio = b.ratio;
    this.promptExtend = b.promptExtend;
    this.watermark = b.watermark;
    this.parameters = b.parameters;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** true when {@code model} routes to the media-based input protocol. */
  public static boolean usesMediaProtocol(String model) {
    if (model == null) {
      return false;
    }
    for (String prefix : MEDIA_MODEL_PREFIXES) {
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

  public String getImageUrl() {
    return imageUrl;
  }

  public List<Map<String, Object>> getMedia() {
    return media;
  }

  public Map<String, Object> getExtraInput() {
    return extraInput;
  }

  public String getFunction() {
    return function;
  }

  public Integer getSeed() {
    return seed;
  }

  public String getSize() {
    return size;
  }

  public Integer getDuration() {
    return duration;
  }

  public String getResolution() {
    return resolution;
  }

  public String getRatio() {
    return ratio;
  }

  public Boolean getPromptExtend() {
    return promptExtend;
  }

  public Boolean getWatermark() {
    return watermark;
  }

  public Map<String, Object> getParameters() {
    return parameters;
  }

  public static final class Builder {
    private String model;
    private String prompt;
    private String imageUrl;
    private List<Map<String, Object>> media;
    private Map<String, Object> extraInput;
    private String function;
    private Integer seed;
    private String size;
    private Integer duration;
    private String resolution;
    private String ratio;
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

    public Builder imageUrl(String imageUrl) {
      this.imageUrl = imageUrl;
      return this;
    }

    public Builder media(List<Map<String, Object>> media) {
      this.media = media;
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

    public Builder size(String size) {
      this.size = size;
      return this;
    }

    public Builder duration(Integer duration) {
      this.duration = duration;
      return this;
    }

    public Builder resolution(String resolution) {
      this.resolution = resolution;
      return this;
    }

    public Builder ratio(String ratio) {
      this.ratio = ratio;
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

    public VideoGenerateParams build() {
      if (model == null || model.isEmpty()) {
        throw new IllegalArgumentException("model is required");
      }
      if (usesMediaProtocol(model)) {
        boolean hasFirstFrame = false;
        if (media != null) {
          for (Map<String, Object> item : media) {
            if (item != null && "first_frame".equals(item.get("type"))) {
              hasFirstFrame = true;
              break;
            }
          }
        }
        if (imageUrl != null && hasFirstFrame) {
          throw new IllegalArgumentException("image_url duplicates a first_frame media item");
        }
        if (imageUrl == null && (media == null || media.isEmpty())) {
          throw new IllegalArgumentException(
              "videos.generate requires media or image_url with " + model);
        }
      } else if (prompt == null
          && imageUrl == null
          && (media == null || media.isEmpty())
          && (extraInput == null || extraInput.isEmpty())
          && function == null) {
        throw new IllegalArgumentException("videos.generate requires prompt or image_url");
      }
      return new VideoGenerateParams(this);
    }
  }
}
