package com.alibaba.dashscope.legacy_2x.aigc.generation;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@Deprecated
public class TranslationOptions {
  /** Full English name of the source language. */
  @SerializedName("source_lang")
  private String sourceLang;

  /** Full English name of the target language. */
  @SerializedName("target_lang")
  private String targetLang;

  /** Domain hints to use when domain prompting is enabled. */
  private String domains;

  /** Terms to use when terminology-based translation intervention is enabled. */
  private List<Term> terms;

  /** Translation memories to use when translation memory is enabled. */
  @SerializedName("tm_list")
  private List<Tm> tmList;

  @SuperBuilder
  @Data
  public static class Tm {
    String source;
    String target;
  }

  @SuperBuilder
  @Data
  public static class Term {
    String source;
    String target;
  }
}
