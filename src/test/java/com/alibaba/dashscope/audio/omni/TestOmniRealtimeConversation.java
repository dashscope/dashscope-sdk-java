// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.audio.omni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.gson.JsonObject;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OmniRealtimeConversation} focusing on WebSocket lifecycle: close()
 * idempotency, onFailure callback notification, and onClosing → checkStatus exception information.
 */
public class TestOmniRealtimeConversation {

  private static class RecordingCallback extends OmniRealtimeCallback {
    final List<JsonObject> events = new ArrayList<>();
    volatile int closeCode = Integer.MIN_VALUE;
    volatile String closeReason = null;
    volatile boolean openCalled = false;
    final AtomicBoolean closeCalled = new AtomicBoolean(false);

    @Override
    public void onOpen() {
      openCalled = true;
    }

    @Override
    public void onEvent(JsonObject message) {
      events.add(message);
    }

    @Override
    public void onClose(int code, String reason) {
      closeCode = code;
      closeReason = reason;
      closeCalled.set(true);
    }
  }

  private OmniRealtimeConversation createConversation(RecordingCallback callback) {
    OmniRealtimeParam param =
        OmniRealtimeParam.builder().model("test-model").apikey("test-key").build();
    return new OmniRealtimeConversation(param, callback);
  }

  private void setConnectLatch(OmniRealtimeConversation conv, CountDownLatch latch)
      throws Exception {
    Field f = OmniRealtimeConversation.class.getDeclaredField("connectLatch");
    f.setAccessible(true);
    f.set(conv, new AtomicReference<>(latch));
  }

  private void setIsClosed(OmniRealtimeConversation conv, boolean value) throws Exception {
    Field f = OmniRealtimeConversation.class.getDeclaredField("isClosed");
    f.setAccessible(true);
    ((AtomicBoolean) f.get(conv)).set(value);
  }

  private void setIsOpen(OmniRealtimeConversation conv, boolean value) throws Exception {
    Field f = OmniRealtimeConversation.class.getDeclaredField("isOpen");
    f.setAccessible(true);
    ((AtomicBoolean) f.get(conv)).set(value);
  }

  private boolean getIsClosed(OmniRealtimeConversation conv) throws Exception {
    Field f = OmniRealtimeConversation.class.getDeclaredField("isClosed");
    f.setAccessible(true);
    return ((AtomicBoolean) f.get(conv)).get();
  }

  private boolean getIsOpen(OmniRealtimeConversation conv) throws Exception {
    Field f = OmniRealtimeConversation.class.getDeclaredField("isOpen");
    f.setAccessible(true);
    return ((AtomicBoolean) f.get(conv)).get();
  }

  @Test
  public void testCloseIdempotent() throws Exception {
    RecordingCallback callback = new RecordingCallback();
    OmniRealtimeConversation conv = createConversation(callback);

    conv.close(1000, "bye");
    assertTrue(getIsClosed(conv));
    assertFalse(getIsOpen(conv));

    // Second close — should be a no-op
    conv.close(1001, "second");
    assertTrue(getIsClosed(conv));
    assertFalse(getIsOpen(conv));
  }

  @Test
  public void testOnFailureCallsCallback() throws Exception {
    RecordingCallback callback = new RecordingCallback();
    OmniRealtimeConversation conv = createConversation(callback);
    setConnectLatch(conv, new CountDownLatch(1));

    Throwable testError = new RuntimeException("connection reset");
    conv.onFailure(null, testError, null);

    assertTrue(callback.closeCalled.get());
    assertEquals(-1, callback.closeCode);
    assertEquals("failure: connection reset", callback.closeReason);
    assertTrue(getIsClosed(conv));
    assertFalse(getIsOpen(conv));
  }

  @Test
  public void testOnClosingCheckStatusThrowsWithInfo() throws Exception {
    RecordingCallback callback = new RecordingCallback();
    OmniRealtimeConversation conv = createConversation(callback);
    setConnectLatch(conv, new CountDownLatch(1));
    setIsOpen(conv, true);

    conv.onClosing(null, 1011, "server error");

    try {
      conv.checkStatus();
      fail("checkStatus should throw RuntimeException after onClosing");
    } catch (RuntimeException e) {
      String msg = e.getMessage();
      assertTrue(msg.contains("already closed"));
      assertTrue(msg.contains("1011"));
      assertTrue(msg.contains("server error"));
    }
  }

  @Test
  public void testCheckStatusNotConnected() {
    RecordingCallback callback = new RecordingCallback();
    OmniRealtimeConversation conv = createConversation(callback);

    try {
      conv.checkStatus();
      fail("checkStatus should throw when not connected");
    } catch (RuntimeException e) {
      assertTrue(e.getMessage().contains("not connected"));
    }
  }

  @Test
  public void testCheckStatusClosedNoInfo() throws Exception {
    RecordingCallback callback = new RecordingCallback();
    OmniRealtimeConversation conv = createConversation(callback);
    setIsClosed(conv, true);

    try {
      conv.checkStatus();
      fail("checkStatus should throw when closed");
    } catch (RuntimeException e) {
      assertTrue(e.getMessage().contains("already closed"));
      assertFalse(e.getMessage().contains("code="));
    }
  }

  @Test
  public void testConnectThrowsWhenClosed() throws Exception {
    RecordingCallback callback = new RecordingCallback();
    OmniRealtimeConversation conv = createConversation(callback);
    setIsClosed(conv, true);

    try {
      conv.connect();
      fail("connect() should throw when already closed");
    } catch (RuntimeException e) {
      assertTrue(e.getMessage().contains("already closed"));
    } catch (Exception e) {
      fail("Expected RuntimeException, got: " + e.getClass().getName());
    }
  }

  @Test
  public void testConnectThrowsWhenAlreadyOpen() throws Exception {
    RecordingCallback callback = new RecordingCallback();
    OmniRealtimeConversation conv = createConversation(callback);
    setIsOpen(conv, true);

    try {
      conv.connect();
      fail("connect() should throw when already connected");
    } catch (RuntimeException e) {
      assertTrue(e.getMessage().contains("already connected"));
    } catch (Exception e) {
      fail("Expected RuntimeException, got: " + e.getClass().getName());
    }
  }

  @Test
  public void testOnClosedCallsCallback() throws Exception {
    RecordingCallback callback = new RecordingCallback();
    OmniRealtimeConversation conv = createConversation(callback);
    setConnectLatch(conv, new CountDownLatch(1));
    setIsOpen(conv, true);

    conv.onClosed(null, 1000, "normal closure");

    assertTrue(callback.closeCalled.get());
    assertEquals(1000, callback.closeCode);
    assertEquals("normal closure", callback.closeReason);
    assertTrue(getIsClosed(conv));
    assertFalse(getIsOpen(conv));
  }

  @Test
  public void testOnFailureReleasesDisconnectLatch() throws Exception {
    RecordingCallback callback = new RecordingCallback();
    OmniRealtimeConversation conv = createConversation(callback);
    setConnectLatch(conv, new CountDownLatch(1));

    CountDownLatch disconnectLatch = new CountDownLatch(1);
    Field f = OmniRealtimeConversation.class.getDeclaredField("disconnectLatch");
    f.setAccessible(true);
    f.set(conv, new AtomicReference<>(disconnectLatch));

    conv.onFailure(null, new RuntimeException("test failure"), null);

    assertEquals(0, disconnectLatch.getCount());
  }
}
