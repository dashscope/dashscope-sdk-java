package com.alibaba.dashscope.tokenizers;

import com.alibaba.dashscope.exception.NoSpecialTokenExists;
import com.alibaba.dashscope.exception.UnSupportedSpecialTokenMode;
import com.alibaba.dashscope.utils.StringUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
  private static final String VOCAB_NAME = "qwen.tiktoken";
  // The vocab is no longer tracked in git; it is downloaded from ModelScope on
  // first use and cached locally (a manual classpath copy is still honored).
  private static final String VOCAB_URL =
      "https://www.modelscope.cn/models/Qwen/Qwen-7B-Chat/resolve/master/" + VOCAB_NAME;
  private static final String VOCAB_SHA256 =
      "b2b1b8dfb5cc5f024bafc373121c6aba3f66f9a5a0269e243470a1de16a33186";
  private static final String VOCAB_ENV = "DASHSCOPE_QWEN_VOCAB";
  private static final Path VOCAB_CACHE_DIR =
      Paths.get(System.getProperty("user.home"), ".cache", "dashscope", "tokenizers");
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
      String specialToken = StringUtils.format("<|extra_%d|>", i);
      map.put(specialToken, specialStartIndex++);
    }
    specialTokens = Collections.unmodifiableMap(map);
  }

  static {
    // ref: https://github.com/openai/tiktoken/blob/main/tiktoken/load.py#L143
    mergeableRanks = new LinkedHashMap<>();
    try (InputStream inputStream = resolveVocab();
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        // 8J+Vkw== 149934 split
        String[] splits = line.split(TOKEN_RANK_SEPARATOR);
        assert splits.length == 2 : "Invalid line in " + VOCAB_NAME + ": " + line;

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
      throw new RuntimeException("Could not load " + VOCAB_NAME, e);
    }
  }

  /**
   * Resolves the vocab from, in order: the {@code DASHSCOPE_QWEN_VOCAB} environment variable, the
   * classpath (optional local copy), a SHA256-verified local cache, or a ModelScope download into
   * {@code ~/.cache/dashscope/tokenizers}.
   */
  static InputStream resolveVocab() throws IOException {
    String override = System.getenv(VOCAB_ENV);
    if (override != null && !override.isEmpty()) {
      return Files.newInputStream(expandHome(override));
    }
    InputStream bundled = QwenTokenizer.class.getClassLoader().getResourceAsStream(VOCAB_NAME);
    if (bundled != null) {
      return bundled;
    }
    Path cached = VOCAB_CACHE_DIR.resolve(VOCAB_NAME);
    if (Files.isRegularFile(cached) && VOCAB_SHA256.equals(sha256(cached))) {
      return Files.newInputStream(cached);
    }
    downloadVocab(cached);
    return Files.newInputStream(cached);
  }

  static Path expandHome(String path) {
    String home = System.getProperty("user.home");
    if (path.equals("~")) {
      return Paths.get(home);
    }
    if (path.startsWith("~/")) {
      return Paths.get(home, path.substring(2));
    }
    return Paths.get(path);
  }

  static String sha256(Path path) throws IOException {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
    try (InputStream in = Files.newInputStream(path)) {
      byte[] buffer = new byte[1 << 20];
      int read;
      while ((read = in.read(buffer)) != -1) {
        digest.update(buffer, 0, read);
      }
    }
    StringBuilder sb = new StringBuilder();
    for (byte b : digest.digest()) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  private static void downloadVocab(Path dest) throws IOException {
    Files.createDirectories(dest.getParent());
    Path tmp = Files.createTempFile(dest.getParent(), VOCAB_NAME + ".", ".download");
    try {
      HttpURLConnection connection =
          (HttpURLConnection) URI.create(VOCAB_URL).toURL().openConnection();
      connection.setConnectTimeout(60_000);
      connection.setReadTimeout(60_000);
      try (InputStream in = connection.getInputStream();
          OutputStream out = Files.newOutputStream(tmp)) {
        byte[] buffer = new byte[1 << 20];
        int read;
        while ((read = in.read(buffer)) != -1) {
          out.write(buffer, 0, read);
        }
      } finally {
        connection.disconnect();
      }
      if (!VOCAB_SHA256.equals(sha256(tmp))) {
        throw new IOException("SHA256 mismatch for vocab downloaded from " + VOCAB_URL);
      }
      try {
        Files.move(tmp, dest, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException | RuntimeException e) {
      throw new RuntimeException(
          "Failed to obtain the Qwen tokenizer vocab ("
              + VOCAB_NAME
              + "). Download it manually from "
              + VOCAB_URL
              + " and either place it at "
              + dest
              + " or point "
              + VOCAB_ENV
              + " at the file (required for offline environments). Cause: "
              + e,
          e);
    } finally {
      Files.deleteIfExists(tmp);
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
        throw new NoSpecialTokenExists(StringUtils.format("No special token in %s", text));
      }
    } else {
      throw new UnSupportedSpecialTokenMode(
          StringUtils.format("UnSupport allowedSpecial: %s", allowedSpecial));
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
