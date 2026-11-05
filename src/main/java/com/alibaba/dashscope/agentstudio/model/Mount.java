package com.alibaba.dashscope.agentstudio.model;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Typed session-resource mount descriptor (create-time input on POST /sessions).
 *
 * <p>Extends {@link LinkedHashMap} so it fits the existing
 * {@code List<Map<String, Object>>} resources parameter without conversion:
 *
 * <pre>{@code
 * List<Map<String, Object>> resources = new ArrayList<>();
 * resources.add(Mount.oss("cn-beijing", "my-bucket", "datasets/", "/data")
 *     .readOnly());
 * resources.add(Mount.bailianMemory("lib_xxx", "customer_001")
 *     .projectIds(Arrays.asList("fact_rule_1"))
 *     .profileSchema("schema_xxx"));
 * }</pre>
 */
public class Mount extends LinkedHashMap<String, Object> {

  private static final long serialVersionUID = 1L;

  public static Mount file(String fileId, String mountPath) {
    return new Mount().putType("file").fileId(fileId).mountPath(mountPath);
  }

  public static Mount memoryStore(String memoryStoreId) {
    return new Mount().putType("memory_store").memoryStoreId(memoryStoreId);
  }

  public static Mount bailianMemory(String memoryStoreId, String entityId) {
    return new Mount()
        .putType("memory_store")
        .provider("agentstudio_memory")
        .memoryStoreId(memoryStoreId)
        .entityId(entityId);
  }

  public static Mount oss(String region, String bucket, String ossPath, String mountPath) {
    return new Mount()
        .putType("oss")
        .region(region)
        .bucket(bucket)
        .ossPath(ossPath)
        .mountPath(mountPath);
  }

  public Mount putType(String type) {
    put("type", type);
    return this;
  }

  public Mount fileId(String fileId) {
    put("file_id", fileId);
    return this;
  }

  public Mount mountPath(String mountPath) {
    put("mount_path", mountPath);
    return this;
  }

  public Mount memoryStoreId(String memoryStoreId) {
    put("memory_store_id", memoryStoreId);
    return this;
  }

  public Mount provider(String provider) {
    put("provider", provider);
    return this;
  }

  public Mount entityId(String entityId) {
    put("entity_id", entityId);
    return this;
  }

  public Mount projectIds(List<String> projectIds) {
    put("project_ids", projectIds);
    return this;
  }

  public Mount profileSchema(String profileSchema) {
    put("profile_schema", profileSchema);
    return this;
  }

  public Mount description(String description) {
    put("description", description);
    return this;
  }

  public Mount access(String access) {
    put("access", access);
    return this;
  }

  public Mount readOnly() {
    return access("read_only");
  }

  public Mount readWrite() {
    return access("read_write");
  }

  public Mount instructions(String instructions) {
    put("instructions", instructions);
    return this;
  }

  public Mount region(String region) {
    put("region", region);
    return this;
  }

  public Mount bucket(String bucket) {
    put("bucket", bucket);
    return this;
  }

  public Mount ossPath(String ossPath) {
    put("oss_path", ossPath);
    return this;
  }
}
