package com.alibaba.dashscope.tools;

public interface ToolAuthenticationBase {
  default ToolAuthenticationBase getAuth() {
    return null;
  }
}
