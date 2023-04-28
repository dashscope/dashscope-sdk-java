package com.dashscope.common;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class Status {

  /** The protocol (like http) status code */
  private int statusCode;

  /** The status message */
  private String message;
}
