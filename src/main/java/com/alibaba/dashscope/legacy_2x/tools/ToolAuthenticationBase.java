package com.alibaba.dashscope.legacy_2x.tools;

@Deprecated
public interface ToolAuthenticationBase {
  default ToolAuthenticationBase getAuth() {
    return null;
  }
}
