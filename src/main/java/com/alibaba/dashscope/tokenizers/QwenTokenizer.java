package com.alibaba.dashscope.tokenizers;

import com.alibaba.dashscope.exception.NoSpecialTokenExists;
import com.alibaba.dashscope.exception.UnSupportedSpecialTokenMode;
import com.alibaba.dashscope.utils.StringUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BPE encode and decode, implementation reference https://github.com/openai/tiktoken and
 * https://github.com/karpathy/minbpe
 */
public class QwenTokenizer implements Tokenizer {
  private static final String SPECIAL_START = "<|";
  private static final String SPECIAL_END = "|>";
  private static final String ENDOFTEXT = "<|endoftext|>";
  private static final String IMSTART = "<|im_start|>";
  private static final String IMEND = "<|im_end|>";
  private static final String PATTEN_STRING =
      "(?i:'s|'t|'re|'ve|'m|'ll|'d)|[^\\r\\n\\p{L}\\p{N}]?\\p{L}+|\\p{N}| ?[^\\s\\p{L}\\p{N}]+[\\r\\n]*|\\s*[\\r\\n]+|\\s+(?!\\S)|\\s+";
  private static final int SPECIAL_START_ID = 151643;
  private static final String TOKEN_RANK_SEPARATOR = " ";
  private static final String vocabularyBpeFile = "qwen.tiktoken";
  private static final Map<EncodeBytesEntity, Integer> mergeableRanks;
  private static final Map<String, Integer> specialTokens;
  private static final byte[][] decodeMap;

  static {
    Map<String, Integer> map = new LinkedHashMap<>();
    int specialStartIndex = SPECIAL_START_ID;
    map.put(ENDOFTEXT, specialStartIndex++);
    map.put(IMSTART, specialStartIndex++);
    map.put(IMEND, specialStartIndex++);
    for (int i = 0; i < 205; i++) {
      String specialToken = String.format("<|extra_%d|>", i);
      map.put(specialToken, specialStartIndex++);
    }
    specialTokens = Collections.unmodifiableMap(map);
  }

  static {
    // ref: https://github.com/openai/tiktoken/blob/main/tiktoken/load.py#L143
    mergeableRanks = new LinkedHashMap<>();
    ClassLoader classLoader = QwenTokenizer.class.getClassLoader();
    try {
      InputStream inputStream = classLoader.getResourceAsStream(vocabularyBpeFile);

      BufferedReader reader =
          new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
      String line;
      while ((line = reader.readLine()) != null) {
        // 8J+Vkw== 149934 split
        String[] splits = line.split(TOKEN_RANK_SEPARATOR);
        assert splits.length == 2 : "Invalid line in " + vocabularyBpeFile + ": " + line;

        byte[] token = Base64.getDecoder().decode(splits[0].getBytes(StandardCharsets.UTF_8));
        int rank = Integer.valueOf(splits[1]);

        mergeableRanks.put(new EncodeBytesEntity(token, rank), rank);
      }
      // init decodeMap
      decodeMap = new byte[mergeableRanks.size() + specialTokens.size()][];
      for (Entry<EncodeBytesEntity, Integer> entry : mergeableRanks.entrySet()) {
        decodeMap[entry.getValue()] =
            Arrays.copyOf(entry.getKey().bytes, entry.getKey().bytes.length);
      }
      for (Entry<String, Integer> entry : specialTokens.entrySet()) {
        byte[] b = entry.getKey().getBytes(StandardCharsets.UTF_8);
        decodeMap[entry.getValue()] = Arrays.copyOf(b, b.length);
      }
    } catch (IOException e) {
      throw new RuntimeException("Could not load " + vocabularyBpeFile + " from resources", e);
    }
  }

  public QwenTokenizer() {}

  private EncodeBytesEntity mergePair(EncodeBytesEntity first, EncodeBytesEntity second) {
    byte[] bytesPair = Arrays.copyOf(first.bytes, first.bytes.length + second.bytes.length);
    System.arraycopy(second.bytes, 0, bytesPair, first.bytes.length, second.bytes.length);
    return new EncodeBytesEntity(bytesPair);
  }

  private EncodeBytesEntity getLowestIndexBytePair(EncodeBytesEntity[] ids) {
    List<EncodeBytesEntity> bytePairs = new ArrayList<>();
    Integer minRank = Integer.MAX_VALUE;
    EncodeBytesEntity minRankPair = null;
    for (int i = 0; i < ids.length - 1; ++i) {
      EncodeBytesEntity bytePair = mergePair(ids[i], ids[i + 1]);
      if (bytePairs.indexOf(bytePair) == -1) {
        Integer rank = mergeableRanks.get(bytePair);
        if (rank == null) {
          bytePair.rank = Integer.MAX_VALUE;
        } else {
          bytePair.rank = rank;
          if (rank < minRank) {
            minRank = rank;
            minRankPair = bytePair;
          }
        }
        bytePairs.add(bytePair);
      }
    }
    return minRankPair;
  }

  private EncodeBytesEntity[] merge(EncodeBytesEntity[] ids, EncodeBytesEntity bytePair) {
    EncodeBytesEntity[] merged = new EncodeBytesEntity[ids.length];
    int mergedIndex = 0;
    for (int i = 0; i < ids.length; ) {
      if (i < ids.length - 1) {
        EncodeBytesEntity mergePair = mergePair(ids[i], ids[i + 1]);
        if (mergePair.equals(bytePair)) {
          merged[mergedIndex++] = bytePair;
          i += 2;
        } else {
          merged[mergedIndex++] = ids[i];
          i += 1;
        }
      } else {
        merged[mergedIndex++] = ids[i];
        i += 1;
      }
    }
    return Arrays.copyOfRange(merged, 0, mergedIndex);
  }

  /**
   * Encode chunk return the token ids.
   *
   * @param chunk the input chunk
   * @return the token ids.
   */
  private List<Integer> encodeChunk(String chunk) {
    byte[] chunkBytes = chunk.getBytes(StandardCharsets.UTF_8);
    EncodeBytesEntity[] ids = new EncodeBytesEntity[chunkBytes.length];
    // convert bytes to integers range 0..255
    int idx = 0;
    for (byte b : chunkBytes) {
      EncodeBytesEntity rankKey = new EncodeBytesEntity(new byte[] {b});
      rankKey.rank = mergeableRanks.get(rankKey);
      ids[idx++] = rankKey;
    }
    List<Integer> tokens = new ArrayList<>();
    if (ids.length < 2) {
      for (EncodeBytesEntity key : ids) {
        tokens.add(key.rank);
      }
      return tokens;
    }
    // merge the byte pair
    while (ids.length >= 2) {
      // find the lowest rank mergeable byte pair
      EncodeBytesEntity bytePair = getLowestIndexBytePair(ids);
      if (bytePair == null) { // no more token can be merged.
        break;
      }
      // merge the lowest merge index
      ids = merge(ids, bytePair);
    }
    for (EncodeBytesEntity key : ids) {
      tokens.add(key.rank);
    }
    return tokens;
  }

  /**
   * Encoding that ignores any special tokens.
   *
   * @param text The input.
   * @return The list of token ids.
   */
  public List<Integer> encodeOrdinary(String text) {
    List<Integer> tokenIds = new ArrayList<>();
    // 1. split the input text to trunks use regex
    Pattern pattern = Pattern.compile(PATTEN_STRING);
    for (Matcher matcher = pattern.matcher(text); matcher.find(); ) {
      // encode the chunk.
      tokenIds.addAll(encodeChunk(matcher.group()));
    }
    return tokenIds;
  }

  private List<String> splitWithSpecial(String text) {
    List<String> chunks = new ArrayList<>();
    if (text.contains(SPECIAL_START) && text.contains(SPECIAL_END)) {
      chunks = StringUtils.splitByStrings(text, specialTokens.keySet());
    } else {
      chunks.add(text);
    }
    return chunks;
  }

  /**
   * Encode the input text, handles special tokens.
   *
   * @param text The input to be encode.
   * @param allowedSpecial The special token options can be "all"|"none"|"none_raise", if
   *     none_raise, then an error is raised if any special token is encountered in text, if null,
   *     use "all"
   * @return The list of token encode.
   * @throws NoSpecialTokenExists No special token in the input.
   * @throws UnSupportedSpecialTokenMode the allowedSpecial is not["all"|"none"|"none_raise"]
   */
  @Override
  public List<Integer> encode(String text, String allowedSpecial)
      throws NoSpecialTokenExists, UnSupportedSpecialTokenMode {
    if (allowedSpecial == null) {
      allowedSpecial = "all";
    }
    Map<String, Integer> specialTokensUse = null;
    if ("all".equals(allowedSpecial)) {
      specialTokensUse = specialTokens;
    } else if ("none".equals(allowedSpecial)) {
      specialTokensUse = new LinkedHashMap<>();
    } else if ("none_raise".equals(allowedSpecial)) {
      specialTokensUse = new LinkedHashMap<>();
      boolean isSpecialTokenExists = false;
      for (String token : specialTokens.keySet()) {
        if (text.indexOf(token) != -1) {
          isSpecialTokenExists = true;
          break;
        }
      }
      if (!isSpecialTokenExists) {
        throw new NoSpecialTokenExists(String.format("No special token in %s", text));
      }
    } else {
      throw new UnSupportedSpecialTokenMode(
          String.format("UnSupport allowedSpecial: %s", allowedSpecial));
    }
    if (specialTokensUse.isEmpty()) {
      // use ordinary encode
      return encodeOrdinary(text);
    }
    // 1. process special tokens. split the text with special tokens.
    // eg: "<|im_start|>system\nYour are a helpful
    // assistant.<|im_end|>\n<|im_start|>user\nSan
    // Francisco is a<|im_end|>\n<|im_start|>assistant\n"
    // will be split to ["<|im_start|>", "system\nYour are a helpful assistant.",
    // "<|im_end|>", "\n", "<|im_start|>", "user\nSan Francisco is a",
    // "<|im_end|>", "\n", "<|im_start|>", "assistant\n"]
    List<String> chunks = splitWithSpecial(text);
    // 2. process the chunks
    List<Integer> tokens = new ArrayList<>();
    for (String chunk : chunks) {
      if (specialTokensUse.containsKey(chunk)) {
        tokens.add(specialTokensUse.get(chunk)); // is special token
      } else {
        tokens.addAll(encodeOrdinary(chunk)); // ordinary inputs
      }
    }
    return tokens;
  }

  @Override
  public String decode(List<Integer> tokens) {
    StringBuilder sb = new StringBuilder();
    for (Integer token : tokens) {
      byte[] bytes = decodeMap[token];
      sb.append(new String(bytes, StandardCharsets.UTF_8));
    }
    return sb.toString();
  }
}
