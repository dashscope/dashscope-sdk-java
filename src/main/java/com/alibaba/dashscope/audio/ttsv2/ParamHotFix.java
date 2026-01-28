// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.ttsv2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

/** Hot fix configuration for speech synthesis, including pronunciation and replace rules. */
@Data
public class ParamHotFix {

  /** Pronunciation rules to customize specific words. */
  private List<PronunciationItem> pronunciation;

  /** Replace rules to replace specific words with others. */
  private List<ReplaceItem> replace;

  public ArrayList<Object> getPronunciation() {
    if (pronunciation == null || pronunciation.isEmpty()) {
      return null;
    }
    ArrayList<Object> pronunciationList = new ArrayList<>();
    for (PronunciationItem item : pronunciation) {
      HashMap<String, String> pronunciationItem = new HashMap<>();
      pronunciationItem.put(item.getText(), item.getPinyin());
      pronunciationList.add(pronunciationItem);
    }

    return pronunciationList;
  }

  public ArrayList<Object> getReplace() {
    if (replace == null || replace.isEmpty()) {
      return null;
    }
    ArrayList<Object> replaceList = new ArrayList<>();
    for (ReplaceItem item : replace) {
      HashMap<String, String> replaceItem = new HashMap<>();
      replaceItem.put(item.getText(), item.getReplacement());
      replaceList.add(replaceItem);
    }

    return replaceList;
  }

  @Data
  @AllArgsConstructor
  public static class PronunciationItem {
    private String text;
    private String pinyin;
  }

  @Data
  @AllArgsConstructor
  public static class ReplaceItem {
    private String text;
    private String replacement;
  }
}
