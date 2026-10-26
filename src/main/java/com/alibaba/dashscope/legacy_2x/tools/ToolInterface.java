package com.alibaba.dashscope.legacy_2x.tools;

public interface ToolInterface {
  public String getType();

  public default ToolAuthenticationBase getAuth() {
    return null;
  }
}
