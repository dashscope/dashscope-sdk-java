// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.resource;

import com.alibaba.dashscope.agentstudio.AgentStudioException;
import com.alibaba.dashscope.agentstudio.message.ContentBlock;
import com.alibaba.dashscope.agentstudio.message.Message;
import com.alibaba.dashscope.common.Status;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
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
                if (data == null || data.isEmpty()) {
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
                AgentStudioException wrapped = wrapFailure(t, response);
                if (wrapped != null) {
                  queue.offer(wrapped);
                } else {
                  queue.offer(POISON);
                }
              }
            });
  }

  /**
   * Convert OkHttp's onFailure into a typed {@link AgentStudioException}: an HTTP {@code response}
   * yields a {@link AgentStudioException.StatusError}; its absence means the transport failed
   * before a response, yielding a {@link AgentStudioException.ConnectionError}.
   */
  private static AgentStudioException wrapFailure(Throwable t, Response response) {
    if (response == null) {
      return t != null ? AgentStudioException.connectionError(t) : null;
    }
    String body = "";
    String errorCode = null;
    String errorMsg = null;
    try (ResponseBody rb = response.body()) {
      if (rb != null) {
        body = rb.string();
      }
    } catch (IOException e) {
      log.debug("Failed to read SSE failure response body", e);
    }
    // Parse the agentstudio error envelope {type:error, error:{code,message}}
    // to surface the server's actual error code instead of a generic one.
    if (body != null && !body.isEmpty()) {
      try {
        JsonObject parsed = GSON.fromJson(body, JsonObject.class);
        if (parsed != null && parsed.has("error") && parsed.get("error").isJsonObject()) {
          JsonObject errObj = parsed.getAsJsonObject("error");
          if (errObj.has("code")) errorCode = errObj.get("code").getAsString();
          if (errObj.has("message")) errorMsg = errObj.get("message").getAsString();
        }
      } catch (Exception e) {
        log.debug("Failed to parse SSE error body as JSON", e);
      }
    }
    // statusError normalizes the parsed code: recognized public codes are kept,
    // anything else falls back to generic api_error.
    String message = errorMsg != null ? errorMsg : (body.isEmpty() ? response.message() : body);
    Status status =
        Status.builder().statusCode(response.code()).code(errorCode).message(message).build();
    return AgentStudioException.statusError(status, t);
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
            throw AgentStudioException.timeout("No event received within " + timeoutMs + "ms");
          }
          if (item == POISON) {
            return false;
          }
          if (item instanceof AgentStudioException) {
            throw (AgentStudioException) item; // already typed by onFailure
          }
          if (item instanceof Throwable) {
            throw AgentStudioException.streamError((Throwable) item); // JSON parse failure
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

  public TextDeltaStream textDeltas() {
    return new TextDeltaStream(this);
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
                  || "rescheduled".equals(status)
                  || "deleted".equals(status)) {
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

  /**
   * Incremental text chunks from {@code event_delta} frames. Requires the stream to be opened with
   * {@code event_deltas}; otherwise yields nothing (use {@link TextStream} for terminal full text).
   * Stops on terminal {@code session_status}.
   */
  public static class TextDeltaStream implements Iterable<String>, Closeable {
    private final AgentStudioEventStream source;

    TextDeltaStream(AgentStudioEventStream source) {
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
              String status = TextStream.extractSessionStatus(msg);
              if (status != null
                  && ("idle".equals(status)
                      || "terminated".equals(status)
                      || "rescheduled".equals(status)
                      || "deleted".equals(status))) {
                done = true;
                return false;
              }
            }
            if ("event_delta".equals(type)) {
              String text = msg.getDeltaText();
              if (text != null && !text.isEmpty()) {
                next = text;
                return true;
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

    @Override
    public void close() {
      source.close();
    }
  }
}
