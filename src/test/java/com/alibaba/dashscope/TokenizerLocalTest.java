// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.tokenization.NoSpecialTokenException;
import com.alibaba.dashscope.tokenization.Tokenizer;
import com.alibaba.dashscope.tokenization.TokenizerFactory;
import com.alibaba.dashscope.tokenization.UnsupportedSpecialTokenModeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/** Local BPE tokenizer (3.0-owned, unchecked exceptions). */
class TokenizerLocalTest {

  @Test
  void encodeOrdinaryMatchesPythonReference() {
    Tokenizer tokenizer = TokenizerFactory.qwen();
    List<Integer> ids = tokenizer.encodeOrdinary("You are a helpful assistant.");
    assertEquals(Arrays.asList(2610, 525, 264, 10950, 17847, 13), ids);
  }

  @Test
  void encodeWithSpecialTokensRoundTrips() {
    Tokenizer tokenizer = TokenizerFactory.qwen();
    String prompt =
        "<|im_start|>system\n"
            + "Your are a helpful assistant.<|im_end|>\n"
            + "<|im_start|>user\n"
            + "SanFrancisco is a<|im_end|>\n"
            + "<|im_start|>assistant\n";
    List<Integer> ids = tokenizer.encode(prompt, "all");
    assertEquals(
        Arrays.asList(
            151644, 8948, 198, 7771, 525, 264, 10950, 17847, 13, 151645, 198, 151644, 872, 198,
            23729, 80328, 9464, 374, 264, 151645, 198, 151644, 77091, 198),
        ids);
    assertEquals(prompt, tokenizer.decode(ids));
  }

  /**
   * The byte-level BPE merges tokens on byte boundaries, so one UTF-8 character can span two
   * tokens. Token 58230 holds {@code 20 E5 B0} and token 122 holds {@code BE}: together they are "
   * \u5C3E" (space + "尾"), but each half on its own is an invalid UTF-8 sequence. Decoding must
   * join the bytes first and decode once, otherwise {@code decode} yields two U+FFFD instead of
   * "尾".
   */
  @Test
  void decodeKeepsCharacterSplitAcrossTokenBoundary() {
    Tokenizer tokenizer = TokenizerFactory.qwen();
    assertEquals(" 尾", tokenizer.decode(Arrays.asList(58230, 122)));
  }

  /**
   * Round trip over CJK heavy texts. Whether a character gets split depends on the merges the
   * vocabulary happens to contain, so the corpus is checked as a whole and every corrupted entry is
   * reported together.
   */
  @Test
  void encodeDecodeRoundTripKeepsEveryCjkCharacter() {
    Tokenizer tokenizer = TokenizerFactory.qwen();
    String[] corpus = {
      "尾",
      "尾部",
      "我的尾巴",
      "多个空格          尾部空格 ",
      "杭州是浙江省的省会。",
      "统计口径以自然月为准",
      "数据库连接失败",
      "请确认收件地址",
      "计算资源不足",
      "输出结果如下",
      "这是一个测试文本，用于验证解码正确性。",
      "模型推理服务极其消耗显存",
      "大模型（LLMs）在许多任务上都有出色的表现，然而大模型推理服务极其消耗显存和算力。",
      "中英文混合 public class Example {}",
      "？+-%^&*\n第二行\t制表符",
    };
    List<String> corrupted = new ArrayList<>();
    for (String text : corpus) {
      List<Integer> ids = tokenizer.encodeOrdinary(text);
      String decoded = tokenizer.decode(ids);
      if (!decoded.equals(text)) {
        corrupted.add(
            escapeUnicode(text)
                + "\n            ids="
                + ids
                + "\n            got="
                + escapeUnicode(decoded));
      }
    }
    assertTrue(
        corrupted.isEmpty(),
        () ->
            corrupted.size()
                + " of "
                + corpus.length
                + " texts lost characters on encode/decode round trip (U+FFFD):\n          "
                + String.join("\n          ", corrupted));
  }

  @Test
  void uncheckedExceptionsReplaceCheckedOnes() {
    Tokenizer tokenizer = TokenizerFactory.qwen();
    String prompt = "Your are a helpful assistant.";
    assertThrows(NoSpecialTokenException.class, () -> tokenizer.encode(prompt, "none_raise"));
    assertThrows(
        UnsupportedSpecialTokenModeException.class, () -> tokenizer.encode(prompt, "hello"));
  }

  private static String escapeUnicode(String text) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c >= 0x20 && c <= 0x7E) {
        sb.append(c);
      } else {
        sb.append(String.format(Locale.ROOT, "\\u%04X", (int) c));
      }
    }
    return sb.toString();
  }
}
