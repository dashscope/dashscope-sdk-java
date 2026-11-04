package com.alibaba.dashscope.legacy_2x.tools;

@Deprecated
public interface ToolInterface {
  public String getType();

  public default ToolAuthenticationBase getAuth() {
    return null;
  }
}
