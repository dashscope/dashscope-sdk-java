// Copyright (c) Alibaba, Inc. and its affiliates.
import java.util.List;
import com.alibaba.dashscope.exception.NoSpecialTokenExists;
import com.alibaba.dashscope.exception.UnSupportedSpecialTokenMode;
import com.alibaba.dashscope.tokenizers.Tokenizer;
import com.alibaba.dashscope.tokenizers.TokenizerFactory;

public class QwenLocalTokenizerUsage {
  public static void testEncodeOrdinary(){
    Tokenizer tokenizer = TokenizerFactory.qwen();
    String prompt ="如果现在要你走十万八千里路，需要多长的时间才能到达？ ";
    // encode string with no special tokens
    List<Integer> ids = tokenizer.encodeOrdinary(prompt);
    System.out.println(ids);
    String decodedString = tokenizer.decode(ids);
    assert decodedString == prompt;
  }

  public static void testEncode() throws NoSpecialTokenExists, UnSupportedSpecialTokenMode{
    Tokenizer tokenizer = TokenizerFactory.qwen();
    String prompt = "<|im_start|>system\nYour are a helpful assistant.<|im_end|>\n<|im_start|>user\nSanFrancisco is a<|im_end|>\n<|im_start|>assistant\n";
    // encode string with special tokens <|im_start|> and <|im_end|>
    List<Integer> ids = tokenizer.encode(prompt, "all");
    // 24 tokens [151644, 8948, 198, 7771, 525, 264, 10950, 17847, 13, 151645, 198, 151644, 872, 198, 23729, 80328, 9464, 374, 264, 151645, 198, 151644, 77091, 198]
    String decodedString = tokenizer.decode(ids);
    System.out.println(ids);
    assert decodedString == prompt;

  }

  public static void main(String[] args) {
      try {
        testEncodeOrdinary();
        testEncode();
      } catch (NoSpecialTokenExists | UnSupportedSpecialTokenMode e) {
        e.printStackTrace();
      }
  }
}