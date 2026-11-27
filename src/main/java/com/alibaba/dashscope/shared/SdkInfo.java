// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.shared;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * SDK version, single-sourced from {@code dashscope-version.properties} (Maven-filtered from
 * pom.xml, design J1). Neutral leaf: JDK-only. 2.x {@code Version} and 3.0 both read here.
 */
public final class SdkInfo {
  private SdkInfo() {}

  private static final String UNKNOWN = "unknown";
  public static final String VERSION = load();

  private static String load() {
    Properties props = new Properties();
    try (InputStream in =
        SdkInfo.class.getClassLoader().getResourceAsStream("dashscope-version.properties")) {
      if (in == null) {
        return UNKNOWN;
      }
      props.load(in);
    } catch (IOException e) {
      return UNKNOWN;
    }
    String v = props.getProperty("version");
    return (v == null || v.isEmpty() || v.contains("${")) ? UNKNOWN : v;
  }
}
