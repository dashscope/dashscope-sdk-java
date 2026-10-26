package com.alibaba.dashscope.legacy_2x.tools;

public interface ToolAuthenticationBase {
  default ToolAuthenticationBase getAuth() {
    return null;
  }
}
