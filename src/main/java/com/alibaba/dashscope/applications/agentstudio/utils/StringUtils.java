// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.applications.agentstudio.utils;

import java.util.Locale;

/** String formatting helpers for the AgentStudio HTTP stack. */
public class StringUtils {
  /**
   * Locale-independent String.format using Locale.ROOT to guarantee ASCII output for numeric format
   * specifiers (%d, %f, etc.), preventing non-ASCII characters in HTTP headers, URLs, and other
   * protocol-sensitive strings.
   */
  public static String format(String format, Object... args) {
    return String.format(Locale.ROOT, format, args);
  }
}
