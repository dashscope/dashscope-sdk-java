package com.alibaba.dashscope.aigc.multimodalconversation;

import lombok.Data;

@Data
public class MultiModalMessageItemText implements MultiModalMessageItemBase {
  private String text;

  public MultiModalMessageItemText(String text) {
    this.text = text;
  }

  @Override
  public String getModal() {
    return "text";
  }

  @Override
  public String getContent() {
    return text;
  }

  @Override
  public void setContent(String content) {
    this.text = content;
  }
}
