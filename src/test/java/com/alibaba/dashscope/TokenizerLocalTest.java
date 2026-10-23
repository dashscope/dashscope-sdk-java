// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alibaba.dashscope.v3.tokenization.NoSpecialTokenException;
import com.alibaba.dashscope.v3.tokenization.Tokenizer;
import com.alibaba.dashscope.v3.tokenization.TokenizerFactory;
import com.alibaba.dashscope.v3.tokenization.UnsupportedSpecialTokenModeException;
import java.util.Arrays;
import java.util.List;
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

  @Test
  void uncheckedExceptionsReplaceCheckedOnes() {
    Tokenizer tokenizer = TokenizerFactory.qwen();
    String prompt = "Your are a helpful assistant.";
    assertThrows(NoSpecialTokenException.class, () -> tokenizer.encode(prompt, "none_raise"));
    assertThrows(
        UnsupportedSpecialTokenModeException.class, () -> tokenizer.encode(prompt, "hello"));
  }
}
