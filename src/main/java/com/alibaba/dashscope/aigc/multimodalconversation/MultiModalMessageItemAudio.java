package com.alibaba.dashscope.aigc.multimodalconversation;

import lombok.Data;

@Data
public class MultiModalMessageItemAudio implements MultiModalMessageItemBase {
  private String audio;

  public MultiModalMessageItemAudio(String audioUrl) {
    this.audio = audioUrl;
  }

  @Override
  public String getModal() {
    return "audio";
  }

  @Override
  public String getContent() {
    return audio;
  }

  @Override
  public void setContent(String content) {
    this.audio = content;
  }
}
