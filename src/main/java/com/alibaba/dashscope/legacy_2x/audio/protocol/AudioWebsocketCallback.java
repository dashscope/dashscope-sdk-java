package com.alibaba.dashscope.legacy_2x.audio.protocol;

import java.nio.ByteBuffer;
import okhttp3.WebSocket;

/**
 * @author songsong.shao
 */
@Deprecated
public interface AudioWebsocketCallback {

  void onOpen();

  void onMessage(WebSocket webSocket, String text);

  void onMessage(WebSocket webSocket, ByteBuffer buffer);

  void onError(WebSocket webSocket, Throwable t);

  void onClose(int code, String reason);
}
