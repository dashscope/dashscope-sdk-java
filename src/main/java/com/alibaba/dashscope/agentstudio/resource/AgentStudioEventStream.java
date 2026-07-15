// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.resource;

import com.alibaba.dashscope.agentstudio.message.ContentBlock;
import com.alibaba.dashscope.agentstudio.message.Message;
import com.alibaba.dashscope.common.Status;
import com.alibaba.dashscope.exception.ApiException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

@Slf4j
public class AgentStudioEventStream implements Iterable<Message>, Closeable {
  private static final Message POISON = new Message();
  private static final Gson GSON =
      new GsonBuilder()
          .registerTypeAdapter(ContentBlock.class, new ContentBlock.Deserializer())
          .create();

  private final BlockingQueue<Object> queue = new LinkedBlockingQueue<>();
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final EventSource eventSource;
  private final long timeoutMs;

  public AgentStudioEventStream(OkHttpClient client, Request request, long timeoutMs) {
    this.timeoutMs = timeoutMs;
    EventSource.Factory factory = EventSources.createFactory(client);
    this.eventSource =
        factory.newEventSource(
            request,
            new EventSourceListener() {
              @Override
              public void onEvent(EventSource es, String id, String type, String data) {
                if (closed.get()) return;
                if (data == null || data.isEmpty() || "{}".equals(data.trim())) {
                  return;
                }
                try {
                  Message msg = GSON.fromJson(data, Message.class);
                  if (msg != null && msg.getType() != null) {
                    queue.put(msg);
                  }
                } catch (Exception e) {
                  queue.offer(e);
                }
              }

              @Override
              public void onClosed(EventSource es) {
                queue.offer(POISON);
              }

              @Override
              public void onFailure(EventSource es, Throwable t, Response response) {
                if (closed.get()) return;
                ApiException wrapped = wrapFailure(t, response);
                if (wrapped != null) {
                  queue.offer(wrapped);
                } else {
                  queue.offer(POISON);
                }
              }
            });
  }

  /** Convert OkHttp's onFailure into an ApiException that preserves HTTP status and body. */
  private static ApiException wrapFailure(Throwable t, Response response) {
    if (response == null) {
      if (t != null) {
        return new ApiException(t instanceof Exception ? (Exception) t : new RuntimeException(t));
      }
      return null;
    }
    int code = response.code();
    String body = "";
    try (ResponseBody rb = response.body()) {
      if (rb != null) {
        body = rb.string();
      }
    } catch (IOException e) {
      log.debug("Failed to read SSE failure response body", e);
    }

    // Try to extract original error code and message from response body
    String apiCode = "";
    String apiMessage = body;
    try {
      com.google.gson.JsonObject json = com.alibaba.dashscope.utils.JsonUtils.parse(body);
      if (json.has("code")) {
        apiCode = json.get("code").getAsString();
      }
      if (json.has("message")) {
        apiMessage = json.get("message").getAsString();
      }
    } catch (Exception e) {
      log.debug("Failed to parse error response body as JSON", e);
    }

    Status status = Status.builder().statusCode(code).code(apiCode).message(apiMessage).build();
    return new ApiException(status, t);
  }

  @Override
  public Iterator<Message> iterator() {
    return new Iterator<Message>() {
      private Message next;

      @Override
      public boolean hasNext() {
        if (next != null) return true;
        if (closed.get()) return false;
        try {
          Object item = queue.poll(timeoutMs, TimeUnit.MILLISECONDS);
          if (item == null) {
            // Differentiate timeout from real end-of-stream: POISON is real EOF, null is timeout.
            throw new ApiException(
                Status.builder()
                    .statusCode(-1)
                    .code("stream_timeout")
                    .message("No event received within " + timeoutMs + "ms")
                    .build());
          }
          if (item == POISON) {
            return false;
          }
          if (item instanceof Throwable) {
            throw new ApiException((Throwable) item);
          }
          next = (Message) item;
          return true;
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return false;
        }
      }

      @Override
      public Message next() {
        if (!hasNext()) throw new NoSuchElementException();
        Message result = next;
        next = null;
        return result;
      }
    };
  }

  public TextStream textStream() {
    return new TextStream(this);
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      eventSource.cancel();
    }
  }

  public boolean isClosed() {
    return closed.get();
  }

  public static class TextStream implements Iterable<String>, Closeable {
    private final AgentStudioEventStream source;

    TextStream(AgentStudioEventStream source) {
      this.source = source;
    }

    @Override
    public Iterator<String> iterator() {
      final Iterator<Message> msgIter = source.iterator();
      return new Iterator<String>() {
        private String next;
        private boolean done;

        @Override
        public boolean hasNext() {
          if (next != null) return true;
          if (done) return false;
          while (msgIter.hasNext()) {
            Message msg = msgIter.next();
            String type = msg.getType();
            if ("session_status".equals(type)) {
              String status = extractSessionStatus(msg);
              if ("idle".equals(status)
                  || "terminated".equals(status)
                  || "rescheduling".equals(status)) {
                done = true;
                return false;
              }
            }
            if ("message".equals(type) && "assistant".equals(msg.getRole())) {
              if (msg.getContent() != null) {
                for (ContentBlock block : msg.getContent()) {
                  if (block instanceof ContentBlock.Text) {
                    String text = ((ContentBlock.Text) block).getText();
                    if (text != null && !text.isEmpty()) {
                      next = text;
                      return true;
                    }
                  }
                }
              }
            }
          }
          done = true;
          return false;
        }

        @Override
        public String next() {
          if (!hasNext()) throw new NoSuchElementException();
          String result = next;
          next = null;
          return result;
        }
      };
    }

    private static String extractSessionStatus(Message msg) {
      if (msg.getContent() != null) {
        for (ContentBlock block : msg.getContent()) {
          if (block instanceof ContentBlock.DataContent) {
            ContentBlock.DataContent dataBlock = (ContentBlock.DataContent) block;
            if (dataBlock.getData() != null
                && dataBlock.getData().has("session_status")
                && !dataBlock.getData().get("session_status").isJsonNull()) {
              return dataBlock.getData().get("session_status").getAsString();
            }
          }
        }
      }
      return null;
    }

    @Override
    public void close() {
      source.close();
    }
  }
}
