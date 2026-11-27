package com.alibaba.dashscope.legacy_2x.aigc.multimodalconversation;

import lombok.Data;

@Data
@Deprecated
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
