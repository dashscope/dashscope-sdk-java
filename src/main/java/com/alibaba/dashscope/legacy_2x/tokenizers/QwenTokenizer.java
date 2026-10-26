package com.alibaba.dashscope.legacy_2x.tokenizers;

import com.alibaba.dashscope.legacy_2x.exception.NoSpecialTokenExists;
import com.alibaba.dashscope.legacy_2x.exception.UnSupportedSpecialTokenMode;
import com.alibaba.dashscope.tokenization.NoSpecialTokenException;
import com.alibaba.dashscope.tokenization.UnsupportedSpecialTokenModeException;
import java.util.List;

/**
 * 2.x compatibility shim: the implementation moved to the 3.0-owned {@link
 * com.alibaba.dashscope.tokenization.QwenTokenizer}; this class delegates and translates the 3.0
 * unchecked exceptions back to the 2.x checked ones.
 */
public class QwenTokenizer implements Tokenizer {
  private final com.alibaba.dashscope.tokenization.QwenTokenizer delegate =
      new com.alibaba.dashscope.tokenization.QwenTokenizer();

  @Override
  public List<Integer> encodeOrdinary(String text) {
    return delegate.encodeOrdinary(text);
  }

  @Override
  public List<Integer> encode(String text, String allowedSpecial)
      throws NoSpecialTokenExists, UnSupportedSpecialTokenMode {
    try {
      return delegate.encode(text, allowedSpecial);
    } catch (NoSpecialTokenException e) {
      throw new NoSpecialTokenExists(e.getMessage());
    } catch (UnsupportedSpecialTokenModeException e) {
      throw new UnSupportedSpecialTokenMode(e.getMessage());
    }
  }

  @Override
  public String decode(List<Integer> tokens) {
    return delegate.decode(tokens);
  }
}
