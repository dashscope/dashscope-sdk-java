// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.shared;

import java.util.UUID;

/**
 * Process-unique SDK session id (design J2). Neutral leaf: JDK-only. Both 2.x
 * ({@code protocol/DashScopeHeaders}) and 3.0 ({@code transport/UserAgentInterceptor}) read the
 * same instance, so one process reports one session id across generations.
 */
public final class SdkSession {
  private SdkSession() {}

  public static final String ID = UUID.randomUUID().toString();
}
