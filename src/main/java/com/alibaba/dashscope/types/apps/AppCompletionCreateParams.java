// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.apps;

import com.alibaba.dashscope.types.messages.Message;
import java.util.List;
import java.util.Map;

/**
 * Parameters for {@code apps().completions().create / createStreaming}. Builder validates required
 * fields at {@code build()} (design §3). Wire shape: {@code {input:{prompt|messages, ...},
 * parameters:{...}}} — known model knobs land in {@code parameters}; unknown passthrough only via
 * {@link Builder#parameters(Map)}.
 *
 * <p>Send {@code prompt} for one turn, {@code messages} for a transcript the caller maintains, or
 * both — {@code prompt} is then appended as the latest user turn. {@code sessionId} / {@code
 * memoryId} ask the service to keep the conversation state instead.
 */
public final class AppCompletionCreateParams {
  private final String appId;
  private final String prompt;
  private final List<Message> messages;
  private final String sessionId;
  private final String docReferenceType;
  private final List<String> docTagCodes;
  private final String memoryId;
  private final Map<String, Object> bizParams;
  private final List<String> imageList;
  private final List<String> fileList;
  private final Double temperature;
  private final Double topP;
  private final Integer topK;
  private final Integer seed;
  private final Boolean hasThoughts;
  private final Map<String, Object> ragOptions;
  private final Boolean incrementalOutput;
  private final Map<String, Object> parameters;

  private AppCompletionCreateParams(Builder b) {
    this.appId = b.appId;
    this.prompt = b.prompt;
    this.messages = b.messages;
    this.sessionId = b.sessionId;
    this.docReferenceType = b.docReferenceType;
    this.docTagCodes = b.docTagCodes;
    this.memoryId = b.memoryId;
    this.bizParams = b.bizParams;
    this.imageList = b.imageList;
    this.fileList = b.fileList;
    this.temperature = b.temperature;
    this.topP = b.topP;
    this.topK = b.topK;
    this.seed = b.seed;
    this.hasThoughts = b.hasThoughts;
    this.ragOptions = b.ragOptions;
    this.incrementalOutput = b.incrementalOutput;
    this.parameters = b.parameters;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getAppId() {
    return appId;
  }

  public String getPrompt() {
    return prompt;
  }

  public List<Message> getMessages() {
    return messages;
  }

  public String getSessionId() {
    return sessionId;
  }

  public String getDocReferenceType() {
    return docReferenceType;
  }

  public List<String> getDocTagCodes() {
    return docTagCodes;
  }

  public String getMemoryId() {
    return memoryId;
  }

  public Map<String, Object> getBizParams() {
    return bizParams;
  }

  public List<String> getImageList() {
    return imageList;
  }

  public List<String> getFileList() {
    return fileList;
  }

  public Double getTemperature() {
    return temperature;
  }

  public Double getTopP() {
    return topP;
  }

  public Integer getTopK() {
    return topK;
  }

  public Integer getSeed() {
    return seed;
  }

  public Boolean getHasThoughts() {
    return hasThoughts;
  }

  public Map<String, Object> getRagOptions() {
    return ragOptions;
  }

  public Boolean getIncrementalOutput() {
    return incrementalOutput;
  }

  public Map<String, Object> getParameters() {
    return parameters;
  }

  public static final class Builder {
    private String appId;
    private String prompt;
    private List<Message> messages;
    private String sessionId;
    private String docReferenceType;
    private List<String> docTagCodes;
    private String memoryId;
    private Map<String, Object> bizParams;
    private List<String> imageList;
    private List<String> fileList;
    private Double temperature;
    private Double topP;
    private Integer topK;
    private Integer seed;
    private Boolean hasThoughts;
    private Map<String, Object> ragOptions;
    private Boolean incrementalOutput;
    private Map<String, Object> parameters;

    private Builder() {}

    public Builder appId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder prompt(String prompt) {
      this.prompt = prompt;
      return this;
    }

    public Builder messages(List<Message> messages) {
      this.messages = messages;
      return this;
    }

    public Builder sessionId(String sessionId) {
      this.sessionId = sessionId;
      return this;
    }

    public Builder docReferenceType(String docReferenceType) {
      this.docReferenceType = docReferenceType;
      return this;
    }

    public Builder docTagCodes(List<String> docTagCodes) {
      this.docTagCodes = docTagCodes;
      return this;
    }

    public Builder memoryId(String memoryId) {
      this.memoryId = memoryId;
      return this;
    }

    /** The application's own flow and plugin inputs. */
    public Builder bizParams(Map<String, Object> bizParams) {
      this.bizParams = bizParams;
      return this;
    }

    public Builder imageList(List<String> imageList) {
      this.imageList = imageList;
      return this;
    }

    public Builder fileList(List<String> fileList) {
      this.fileList = fileList;
      return this;
    }

    public Builder temperature(Double temperature) {
      this.temperature = temperature;
      return this;
    }

    public Builder topP(Double topP) {
      this.topP = topP;
      return this;
    }

    public Builder topK(Integer topK) {
      this.topK = topK;
      return this;
    }

    public Builder seed(Integer seed) {
      this.seed = seed;
      return this;
    }

    /** Ask for planning / tool-call steps in {@code thoughts}. */
    public Builder hasThoughts(Boolean hasThoughts) {
      this.hasThoughts = hasThoughts;
      return this;
    }

    public Builder ragOptions(Map<String, Object> ragOptions) {
      this.ragOptions = ragOptions;
      return this;
    }

    public Builder incrementalOutput(Boolean incrementalOutput) {
      this.incrementalOutput = incrementalOutput;
      return this;
    }

    /** Passthrough for model knobs this signature does not name. */
    public Builder parameters(Map<String, Object> parameters) {
      this.parameters = parameters;
      return this;
    }

    public AppCompletionCreateParams build() {
      if (appId == null || appId.isEmpty()) {
        throw new IllegalArgumentException("app_id is required");
      }
      if ((prompt == null || prompt.isEmpty()) && (messages == null || messages.isEmpty())) {
        throw new IllegalArgumentException("prompt or messages is required");
      }
      return new AppCompletionCreateParams(this);
    }
  }
}
