package com.alibaba.dashscope.tools;

public interface ToolInterface {
  public String getType();

  public default ToolAuthenticationBase getAuth() {
    return null;
  }
}
