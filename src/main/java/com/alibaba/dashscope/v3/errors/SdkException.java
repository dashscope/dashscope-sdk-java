// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.errors;

/**
 * A failure the SDK reported itself, with no HTTP response behind it. Carries the catalog's
 * {@code solution} (fix guidance) and {@code allowRetry}. {@code statusCode} stays null: the
 * catalog gives SDK-side errors no public projection.
 */
public class SdkException extends DashScopeException {
  private final String solution;
  private final boolean allowRetry;

  public SdkException(String message, String code) {
    this(message, code, null, false, null);
  }

  public SdkException(
      String message, String code, String solution, boolean allowRetry, Throwable cause) {
    super(message, code, null, null, null, null, cause);
    this.solution = solution;
    this.allowRetry = allowRetry;
  }

  /** Fix guidance from the SDK error catalog; nullable. */
  public String getSolution() {
    return solution;
  }

  public boolean isAllowRetry() {
    return allowRetry;
  }

  @Override
  public String toString() {
    String base = super.toString();
    if (solution != null && !solution.isEmpty()) {
      base += " | solution=" + solution;
    }
    return base;
  }
}
