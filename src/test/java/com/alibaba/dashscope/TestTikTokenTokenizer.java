package com.alibaba.dashscope;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import com.alibaba.dashscope.exception.NoSpecialTokenExists;
import com.alibaba.dashscope.exception.UnSupportedSpecialTokenMode;
import com.alibaba.dashscope.tokenizers.Tokenizer;
import com.alibaba.dashscope.tokenizers.TokenizerFactory;
import com.alibaba.dashscope.utils.JsonUtils;
import com.alibaba.dashscope.utils.StringUtils;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Ignore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class TestTikTokenTokenizer {
  @Test
  public void testEncodeOrdinary() throws UnSupportedSpecialTokenMode {
    Tokenizer tokenizer = TokenizerFactory.qwen();
    String prompt = "You are a helpful assistant.";
    List<Integer> ids = tokenizer.encodeOrdinary(prompt);
    // You are a helpful assistant.
    // [2610, 525, 264, 10950, 17847, 13]
    assertTrue(ids.equals(Arrays.asList(2610, 525, 264, 10950, 17847, 13)));
  }

  @Test
  public void testSplitByStrings() throws NoSpecialTokenExists, UnSupportedSpecialTokenMode {
    String prompt =
        "<|im_start|>system\nYour are a helpful assistant.<|im_end|>\n<|im_start|>user\nSanFrancisco is a<|im_end|>\n<|im_start|>assistant\n";
    List<String> spliters = Arrays.asList("<|im_start|>", "<|im_end|>");
    List<String> chunks = StringUtils.splitByStrings(prompt, spliters);
    for (String chunk : chunks) {
      System.out.println(chunk);
    }
    List<String> expect =
        Arrays.asList(
            "<|im_start|>",
            "system\nYour are a helpful assistant.",
            "<|im_end|>",
            "\n",
            "<|im_start|>",
            "user\nSanFrancisco is a",
            "<|im_end|>",
            "\n",
            "<|im_start|>",
            "assistant\n");
    assert (expect.equals(chunks));
  }

  @Test
  public void testEncode() throws NoSpecialTokenExists, UnSupportedSpecialTokenMode {
    Tokenizer tokenizer = TokenizerFactory.qwen();
    String prompt =
        "<|im_start|>system\nYour are a helpful assistant.<|im_end|>\n<|im_start|>user\nSanFrancisco is a<|im_end|>\n<|im_start|>assistant\n";
    List<Integer> ids = tokenizer.encode(prompt, "all");
    // python: 24 tokens [151644, 8948, 198, 7771, 525, 264, 10950, 17847, 13,
    // 151645, 198, 151644,
    // 872, 198, 23729, 80328, 9464, 374, 264, 151645, 198, 151644, 77091, 198]
    assertArrayEquals(
        ids.toArray(),
        new Integer[] {
          151644, 8948, 198, 7771, 525, 264, 10950, 17847, 13, 151645, 198, 151644, 872, 198, 23729,
          80328, 9464, 374, 264, 151645, 198, 151644, 77091, 198
        });
    String decodedString = tokenizer.decode(ids);
    assertTrue(decodedString.equals(prompt));
    prompt = "大模型（LLMs）在许多任务上都有出色的表现，然而大模型推理服务极其消耗显存和算力。"; // no special token.
    ids = tokenizer.encode(prompt, "all");
    decodedString = tokenizer.decode(ids);
    assertTrue(decodedString.equals(prompt));
  }

  @Test
  public void testEncodeExceptions() {
    Tokenizer tokenizer = TokenizerFactory.qwen();
    String prompt = "Your are a helpful assistant.";
    assertThrows(
        NoSpecialTokenExists.class,
        () -> {
          tokenizer.encode(prompt, "none_raise");
        });
    assertThrows(
        UnSupportedSpecialTokenMode.class,
        () -> {
          tokenizer.encode(prompt, "hello");
        });
  }

  @Ignore("Develop use to compare python and java difference")
  @Disabled
  @Test
  public void testEncodeMultipleInput() {
    Tokenizer tokenizer = TokenizerFactory.qwen();
    String inputFilePath = "./src/test/resources/test_tokenizer_prompts.jsonl";
    String pythonEncodeResultFilePath =
        "src/test/resources/test_tokenizer_prompts_python_encode.jsonl";
    try {
      List<String> lines = Files.readAllLines(Paths.get(inputFilePath), StandardCharsets.UTF_8);
      List<String> pythonEncodeResult = Files.readAllLines(Paths.get(pythonEncodeResultFilePath));
      int index = 0;
      String mismatchResultFilePath = "src/test/resources/mismatch.jsonl";
      FileWriter fstream =
          new FileWriter(mismatchResultFilePath, true); // true tells to append data.
      BufferedWriter out = new BufferedWriter(fstream);
      for (String line : lines) {
        JsonObject item = JsonUtils.parse(line);
        String question = item.get("question").getAsString();
        String humanAnswers = item.get("human_answers").getAsJsonArray().get(0).getAsString();
        String chatgptAnswers = item.get("chatgpt_answers").getAsJsonArray().get(0).getAsString();
        List<Integer> questionTokens = tokenizer.encode(question, "all");
        List<Integer> humanAnswerTokens = tokenizer.encode(humanAnswers, "all");
        List<Integer> chatgptAnswerTokens = tokenizer.encode(chatgptAnswers, "all");
        JsonObject resultItem = JsonUtils.parse(pythonEncodeResult.get(index));
        Type listType = new TypeToken<ArrayList<Integer>>() {}.getType();
        List<Integer> pythonQuestionTokens =
            JsonUtils.fromJson(resultItem.get("question").getAsJsonArray(), listType);
        List<Integer> pythonHumanAnswerTokens =
            JsonUtils.fromJson(resultItem.get("human_answers").getAsJsonArray(), listType);
        List<Integer> pythonChatgptAnswerTokens =
            JsonUtils.fromJson(resultItem.get("chatgpt_answers").getAsJsonArray(), listType);
        if (!questionTokens.equals(pythonQuestionTokens)) {
          String msg =
              String.format(
                  "{\"Question\": \"%s\", \"Python\": %s, \"Java\": %s}",
                  question, pythonQuestionTokens, questionTokens);
          System.out.println(msg);
          out.write(msg + "\n");
        }
        if (!humanAnswerTokens.equals(pythonHumanAnswerTokens)) {
          String msg =
              String.format(
                  "{\"HumanAnswer\": \"%s\", \"Python\": %s, \"Java\": %s}",
                  humanAnswers, pythonHumanAnswerTokens, humanAnswerTokens);
          System.out.println(msg);
          out.write(msg + "\n");
        }
        if (!chatgptAnswerTokens.equals(pythonChatgptAnswerTokens)) {
          String msg =
              String.format(
                  "{\"ChatgptAnswer\": \"%s\", \"Python\": %s, \"Java\": %s}",
                  chatgptAnswers, pythonChatgptAnswerTokens, chatgptAnswerTokens);
          System.out.println(msg);
          out.write(msg + "\n");
        }
        ++index;
      }
      out.close();
    } catch (IOException | NoSpecialTokenExists e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (UnSupportedSpecialTokenMode e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Test
  public void testSplitByString() {
    String s = "<|im_start|>system<|im_start|>Your are a helpful assistant.<|im_start|>";
    List<String> parts = StringUtils.splitByString(s, "<|im_start|>");
    assert parts.size() == 5;
    s = "<|im_start|>";
    parts = StringUtils.splitByString(s, "<|im_start|>");
    assert parts.size() == 1;
    s = "<|im_start|><|im_start|>";
    parts = StringUtils.splitByString(s, "<|im_start|>");
    assert parts.size() == 2;
  }
}
