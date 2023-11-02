package com.alibaba.dashscope.aigc.multimodalconversation;

import lombok.Data;

@Data
public class MultiModalMessageItemImage implements MultiModalMessageItemBase {
  private String image;

  public MultiModalMessageItemImage(String imageUrl) {
    this.image = imageUrl;
  }

  @Override
  public String getModal() {
    return "image";
  }

  @Override
  public String getContent() {
    return image;
  }

  @Override
  public void setContent(String content) {
    this.image = content;
  }
}
