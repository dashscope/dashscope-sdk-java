// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.protocol;

import com.alibaba.dashscope.utils.Constants;
import java.util.Arrays;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public final class ClientOptions {
  // enable detail log of the communication.
  // [NONE, BASIC, HEADERS, BODY]
  private static final String DEFAULT_NETWORK_LOGGING_LEVEL = "NONE";
  private static final String DEFAULT_SDK_LOGGING_LEVEL = "WARN";
  private String implementation;
  private String networkLoggingLevel;
  private String sdkLoggingLevel;

  public String getNetworkLoggingLevel() {
    if (networkLoggingLevel != null) {
      return networkLoggingLevel;
    } else {
      String logLevel =
          System.getenv()
              .getOrDefault(
                  Constants.DASHSCOPE_NETWORK_LOGGING_LEVEL_ENV, DEFAULT_NETWORK_LOGGING_LEVEL);
      if (Arrays.asList("NONE", "BASIC", "HEADERS", "BODY").contains(logLevel)) {
        return logLevel;
      } else {
        return DEFAULT_NETWORK_LOGGING_LEVEL;
      }
    }
  }

  public String getSdkLoggingLevel() {
    if (sdkLoggingLevel != null) {
      return sdkLoggingLevel;
    } else {
      return System.getenv()
          .getOrDefault(Constants.DASHSCOPE_SDK_LOGGING_LEVEL_ENV, DEFAULT_SDK_LOGGING_LEVEL);
    }
  }
}
