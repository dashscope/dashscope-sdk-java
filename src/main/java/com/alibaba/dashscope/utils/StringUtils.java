package com.alibaba.dashscope.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class StringUtils {
  /**
   * Locale-independent String.format using Locale.ROOT to guarantee ASCII output for numeric format
   * specifiers (%d, %f, etc.), preventing non-ASCII characters in HTTP headers, URLs, and other
   * protocol-sensitive strings.
   */
  public static String format(String format, Object... args) {
    return String.format(Locale.ROOT, format, args);
  }

  /*
   * * Split src with spliter, return the every part of include spliter eg: src
   * "<|im_start|>system", spliter: <|im_start|> the result is: ["<|im_start|>",
   * "system"] used in tokenizer.
   */
  public static List<String> splitByString(String src, String spliter) {
    List<String> parts = new ArrayList<>();
    int from = 0;
    int first = src.indexOf(spliter, from);
    while (first != -1) {
      if (from == first) { // starts with special
        parts.add(spliter);
        from += spliter.length();
      } else {
        parts.add(src.substring(from, first));
        parts.add(spliter);
        from += first - from + spliter.length();
      }
      first = src.indexOf(spliter, from);
    }
    String remain = src.substring(from);
    if (remain.length() > 0) {
      parts.add(src.substring(from));
    }
    return parts;
  }

  /*
   * * Split text by list of string. eg: "<|im_start|>system\nYour are a helpful
   * assistant.<|im_end|>\n<|im_start|>user\nSanFrancisco is
   * a<|im_end|>\n<|im_start|>assistant\n"; spliters: ["<|im_start|>", "<|im_end|>"]
   * result: ["<|im_end|>","system\nYour are a helpful assistant.", "<|im_end|>",
   * "\n","<|im_start|>", "user\nSanFrancisco is a", "<|im_end|>", "\n", "<|im_start|>",
   * "assistant\n" ]
   */
  public static List<String> splitByStrings(String text, Collection<String> spliters) {
    List<String> chunks = new ArrayList<>();
    chunks.add(text);
    for (String specialToken : spliters) {
      List<String> thisSplits = new ArrayList<>();
      for (String chunk : chunks) {
        thisSplits.addAll(splitByString(chunk, specialToken));
      }
      chunks = thisSplits;
    }
    return chunks;
  }
}
