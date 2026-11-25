// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope;

import com.alibaba.dashscope.shared.SdkInfo;

public final class Version {
  // J1: single source is dashscope-version.properties (Maven-filtered from pom.xml);
  // read via the neutral layer so 2.x and 3.0 always report the same version.
  public static final String version = SdkInfo.VERSION;
}
