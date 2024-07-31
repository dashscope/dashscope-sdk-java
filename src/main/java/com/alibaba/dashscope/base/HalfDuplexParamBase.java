// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.base;

import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.utils.GsonExclude;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

/** The user input and parameter. */
@Data
@SuperBuilder
public abstract class HalfDuplexParamBase {
  /** The apiKey. */
  private String apiKey;
  /** Open security check */
  @GsonExclude @Builder.Default private boolean securityCheck = false;

  /** workspace */
  @GsonExclude private String workspace;

  @GsonExclude @Builder.Default private Boolean enableEncrypt = false;

  public String getWorkspace() {
    return workspace;
  }

  public String getApiKey() {
    return apiKey;
  }

  /** The extra parameters. */
  @GsonExclude @Singular protected Map<String, Object> parameters;

  public abstract String getModel();
  /**
   * The service parameters.
   *
   * @return The parameters
   */
  public abstract Map<String, Object> getParameters();

  /** The custom http header. */
  @GsonExclude @Singular protected Map<String, Object> headers;

  /**
   * The custom http header.
   *
   * @return The headers.
   */
  public abstract Map<String, String> getHeaders();

  /**
   * Get the batch request data. Http body include {"model": "model_name", "input": "input data",
   * "parameters": "The request parameters"}
   *
   * @return The http request body
   */
  public abstract JsonObject getHttpBody();

  /**
   * Get the request input data.
   *
   * @return The input data object.
   */
  public abstract Object getInput();

  /**
   * Get the request resources
   *
   * @return The resource object
   */
  public abstract Object getResources();

  /**
   * Get the binary data, only websocket, if no binary data, return null.
   *
   * @return The binary data
   */
  public abstract ByteBuffer getBinaryData();

  /**
   * Validate the input and parameters.
   *
   * @throws InputRequiredException Missing input fields.
   */
  public abstract void validate() throws InputRequiredException;
}
