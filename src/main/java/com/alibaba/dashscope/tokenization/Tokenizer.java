// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.tokenization;

import java.util.List;

/**
 * Local tokenizer contract (3.0 home of the 2.x {@code tokenizers} leaf). Failures are unchecked:
 * {@link NoSpecialTokenException} / {@link UnsupportedSpecialTokenModeException}.
 */
public interface Tokenizer {
  /**
   * Encoding that ignores any special tokens. There is no guarantee that the results will be
   * completely consistent with the model output. Compared with the python version, there are some
   * differences. It is known that the results of processing 2 or 4 consecutive spaces are
   * different. Detailed information for qwen reference:
   * https://github.com/QwenLM/Qwen/blob/main/tokenization_note_zh.md
   *
   * @param text The input.
   * @return The list of token ids.
   */
  List<Integer> encodeOrdinary(String text);

  /**
   * Encode the input text, handles special tokens. Same accuracy caveats as {@link
   * #encodeOrdinary(String)}.
   *
   * @param text The input to be encoded.
   * @param allowedSpecial The special token options, one of "all"|"none"|"none_raise"; with
   *     "none_raise" a {@link NoSpecialTokenException} is thrown when no special token is present;
   *     null means "all".
   * @return The list of token ids.
   * @throws NoSpecialTokenException No special token in the input (none_raise mode).
   * @throws UnsupportedSpecialTokenModeException allowedSpecial is not all/none/none_raise.
   */
  List<Integer> encode(String text, String allowedSpecial);

  /**
   * Decode token ids to String.
   *
   * @param ids The input token ids, eg: [2610, 525, 264, 10950, 17847, 13], will be decoded to
   *     "You are a helpful assistant."
   * @return The token ids corresponding string.
   */
  String decode(List<Integer> ids);
}
