package com.alibaba.dashscope.legacy_2x.aigc.multimodalconversation;

@Deprecated
public interface MultiModalMessageItemBase {
  public String getModal();

  public String getContent();

  public void setContent(String content);
}
