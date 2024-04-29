package com.alibaba.dashscope;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import okhttp3.MediaType;

// copy from spring-mvc
public class ServerSentEvent {
  private static final MediaType TEXT_PLAIN = MediaType.parse("text/plain/ charset=utf-8");

  public static class DataWithMediaType {

    private final Object data;

    private final MediaType mediaType;

    public DataWithMediaType(Object data, MediaType mediaType) {
      this.data = data;
      this.mediaType = mediaType;
    }

    public Object getData() {
      return this.data;
    }

    public MediaType getMediaType() {
      return this.mediaType;
    }
  }

  /** A builder for an SSE event. */
  public interface SseEventBuilderInterface {

    /** Add an SSE "id" line. */
    SseEventBuilderInterface id(String id);

    /** Add an SSE "event" line. */
    SseEventBuilderInterface name(String eventName);

    /** Add an SSE "retry" line. */
    SseEventBuilderInterface reconnectTime(long reconnectTimeMillis);

    /** Add an SSE "comment" line. */
    SseEventBuilderInterface comment(String comment);

    /** Add an SSE "data" line. */
    SseEventBuilderInterface data(Object object);

    /** Add an SSE "data" line. */
    SseEventBuilderInterface data(Object object, MediaType mediaType);

    /**
     * Return one or more Object-MediaType pairs to write via {@link #send(Object, MediaType)}.
     *
     * @since 4.2.3
     */
    Set<DataWithMediaType> build();
  }

  /** Default implementation of SseEventBuilder. */
  public static class SseEventBuilder implements SseEventBuilderInterface {

    private final Set<DataWithMediaType> dataToSend = new LinkedHashSet<>(4);

    private StringBuilder sb;

    @Override
    public SseEventBuilderInterface id(String id) {
      append("id:").append(id).append('\n');
      return this;
    }

    @Override
    public SseEventBuilderInterface name(String name) {
      append("event:").append(name).append('\n');
      return this;
    }

    @Override
    public SseEventBuilderInterface reconnectTime(long reconnectTimeMillis) {
      append("retry:").append(String.valueOf(reconnectTimeMillis)).append('\n');
      return this;
    }

    @Override
    public SseEventBuilderInterface comment(String comment) {
      append(':').append(comment).append('\n');
      return this;
    }

    @Override
    public SseEventBuilderInterface data(Object object) {
      return data(object, null);
    }

    /**
     * Replace all occurrences of a substring within a string with another string.
     *
     * @param inString {@code String} to examine
     * @param oldPattern {@code String} to replace
     * @param newPattern {@code String} to insert
     * @return a {@code String} with the replacements
     */
    public static String replace(String inString, String oldPattern, String newPattern) {
      if ((inString == null || inString.length() == 0)
          || (oldPattern == null || oldPattern.length() == 0)
          || newPattern == null) {
        return inString;
      }
      int index = inString.indexOf(oldPattern);
      if (index == -1) {
        // no occurrence -> can return input as-is
        return inString;
      }

      int capacity = inString.length();
      if (newPattern.length() > oldPattern.length()) {
        capacity += 16;
      }
      StringBuilder sb = new StringBuilder(capacity);

      int pos = 0; // our position in the old string
      int patLen = oldPattern.length();
      while (index >= 0) {
        sb.append(inString, pos, index);
        sb.append(newPattern);
        pos = index + patLen;
        index = inString.indexOf(oldPattern, pos);
      }

      // append any characters to the right of a match
      sb.append(inString, pos, inString.length());
      return sb.toString();
    }

    @Override
    public SseEventBuilderInterface data(Object object, MediaType mediaType) {
      append("data:");
      saveAppendedText();
      if (object instanceof String) {
        String text = (String) object;
        object = replace(text, "\n", "\ndata:");
      }
      this.dataToSend.add(new DataWithMediaType(object, mediaType));
      append('\n');
      return this;
    }

    SseEventBuilder append(String text) {
      if (this.sb == null) {
        this.sb = new StringBuilder();
      }
      this.sb.append(text);
      return this;
    }

    SseEventBuilder append(char ch) {
      if (this.sb == null) {
        this.sb = new StringBuilder();
      }
      this.sb.append(ch);
      return this;
    }

    @Override
    public Set<DataWithMediaType> build() {
      if ((this.sb == null || this.sb.length() == 0) && this.dataToSend.isEmpty()) {
        return Collections.emptySet();
      }
      append('\n');
      saveAppendedText();
      return this.dataToSend;
    }

    private void saveAppendedText() {
      if (this.sb != null) {
        this.dataToSend.add(new DataWithMediaType(this.sb.toString(), TEXT_PLAIN));
        this.sb = null;
      }
    }
  }
}
