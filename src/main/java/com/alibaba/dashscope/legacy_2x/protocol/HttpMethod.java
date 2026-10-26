// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.legacy_2x.protocol;

/** HTTP methods supported by the SDK transport. */
public enum HttpMethod {
  /** Create or invoke an action. */
  POST,

  /** Retrieve a resource. */
  GET,

  /** Replace or update a resource. */
  PUT,

  /** Delete a resource. */
  DELETE;
}
