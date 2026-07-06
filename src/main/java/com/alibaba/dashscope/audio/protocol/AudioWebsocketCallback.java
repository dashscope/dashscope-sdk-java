package com.alibaba.dashscope.audio.protocol;

import java.nio.ByteBuffer;
import okhttp3.WebSocket;

/** @author songsong.shao */
public interface AudioWebsocketCallback {

  void onOpen();

  void onMessage(WebSocket webSocket, String text);

  void onMessage(WebSocket webSocket, ByteBuffer buffer);

  void onError(WebSocket webSocket, Throwable t);

  void onClose(int code, String reason);
}
