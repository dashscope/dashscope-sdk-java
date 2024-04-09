package com.alibaba.dashscope.tokenizers;

import com.alibaba.dashscope.exception.NoSpecialTokenExists;
import com.alibaba.dashscope.exception.UnSupportedSpecialTokenMode;
import java.util.List;

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
  public List<Integer> encodeOrdinary(String text);
  /**
   * Encode the input text, handles special tokens. There is no guarantee that the results will be
   * completely consistent with the model output. Compared with the python version, there are some
   * differences. It is known that the results of processing 2 or 4 consecutive spaces are
   * different. Detailed information for qwen reference:
   * https://github.com/QwenLM/Qwen/blob/main/tokenization_note_zh.md
   *
   * @param text The input to be encode.
   * @param allowedSpecial The special token options can be "all"|"none"|"none_raise", if
   *     none_raise, then an `NoSpecialTokenExists` is throw if any special token is encountered in
   *     text, if null, use "all"
   * @return The list of token encode.
   * @throws NoSpecialTokenExists No special token in the input.
   * @throws UnSupportedSpecialTokenMode the allowedSpecial is not["all"|"none"|"none_raise"]
   */
  List<Integer> encode(String text, String allowedSpecial)
      throws NoSpecialTokenExists, UnSupportedSpecialTokenMode;

  /**
   * Decode token ids to String
   *
   * @param ids The input token ids, eg: [2610, 525, 264, 10950, 17847, 13], will be Decoded to "You
   *     are a helpful assistant."
   * @return The token ids corresponding string.
   */
  String decode(List<Integer> ids);
}
