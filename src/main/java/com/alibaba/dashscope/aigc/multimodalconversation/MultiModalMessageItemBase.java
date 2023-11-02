package com.alibaba.dashscope.aigc.multimodalconversation;

public interface MultiModalMessageItemBase {
  public String getModal();

  public String getContent();

  public void setContent(String content);
}
