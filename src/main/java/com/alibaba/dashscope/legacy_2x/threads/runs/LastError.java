package com.alibaba.dashscope.legacy_2x.threads.runs;

import lombok.Data;

@Data
@Deprecated
public class LastError {
  private String code;
  private String message;
}
