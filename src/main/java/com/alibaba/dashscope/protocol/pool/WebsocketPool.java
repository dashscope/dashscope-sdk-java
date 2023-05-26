package com.alibaba.dashscope.protocol.pool;

import com.alibaba.dashscope.protocol.WebsocketRpc;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.DatatypeConverter;
import lombok.var;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

public class WebsocketPool {

  static class WsPoolFactory extends BasePooledObjectFactory<WebsocketRpc> {

    private final String url;

    private final Map<String, String> headers;

    public WsPoolFactory(String url, Map<String, String> headers) {
      this.url = url;
      this.headers = headers;
    }

    @Override
    public WebsocketRpc create() {
      return new WebsocketRpc(url, headers);
    }

    @Override
    public PooledObject<WebsocketRpc> wrap(WebsocketRpc o) {
      return new DefaultPooledObject<>(o);
    }
  }

  private static volatile WebsocketPool instance;

  private final Map<String, GenericObjectPool<WebsocketRpc>> wsPoolFactoryMap =
      Maps.newConcurrentMap();

  private final Set<WebsocketRpc> borrowed = Sets.newConcurrentHashSet();

  public static WebsocketPool getInstance() {
    if (instance == null) {
      synchronized (HttpPool.class) {
        if (instance == null) {
          instance = new WebsocketPool();
        }
      }
    }
    return instance;
  }

  private WebsocketPool() {}

  @Override
  protected void finalize() throws Throwable {
    for (GenericObjectPool<WebsocketRpc> pool : wsPoolFactoryMap.values()) {
      pool.close();
    }
    borrowed.clear();
    super.finalize();
  }

  public static String calculateMd5(Map<String, String> headers) {
    try {
      List<Map.Entry<String, String>> entries = new ArrayList<>(headers.entrySet());
      entries.sort(Map.Entry.comparingByKey());
      StringBuilder sb = new StringBuilder();
      sb.append("{");
      for (Map.Entry<String, String> entry : entries) {
        if (sb.length() > 1) {
          sb.append(", ");
        }
        sb.append(entry.getKey()).append("=").append(entry.getValue());
      }
      sb.append("}");
      String mapString = sb.toString();
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] hash = md.digest(mapString.getBytes());
      return DatatypeConverter.printHexBinary(hash).toLowerCase();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Get a rpc client from the pool.
   *
   * @param url The connecting url of the websocket client.
   * @param headers The headers to use for connection.
   * @return The websocket rpc client.
   * @throws Exception Error occurs.
   */
  public WebsocketRpc getWsClient(String url, Map<String, String> headers) throws Exception {
    String key = url + calculateMd5(headers);
    if (!wsPoolFactoryMap.containsKey(key)) {
      synchronized (wsPoolFactoryMap) {
        if (!wsPoolFactoryMap.containsKey(key)) {
          wsPoolFactoryMap.put(key, new GenericObjectPool<>(new WsPoolFactory(url, headers)));
        }
      }
    }
    var websocketRpc = wsPoolFactoryMap.get(key).borrowObject();
    borrowed.add(websocketRpc);
    return websocketRpc;
  }

  /**
   * Return the borrowed object to the pool.
   *
   * @param websocketRpc The borrowed websocket rpc client.
   */
  public void returnWsClient(WebsocketRpc websocketRpc) {
    borrowed.remove(websocketRpc);
    wsPoolFactoryMap
        .get(websocketRpc.getUrl() + calculateMd5(websocketRpc.getHeaders()))
        .returnObject(websocketRpc);
  }
}
