// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.errors.BadRequestException;
import com.alibaba.dashscope.types.files.DeletionStatus;
import com.alibaba.dashscope.types.files.FileList;
import com.alibaba.dashscope.types.files.FileObject;
import com.alibaba.dashscope.types.files.FileUploadParams;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

/** MockWebServer tests for {@code files()} (Python v2 files.py parity). */
@ClearEnvironmentVariable(key = "DASHSCOPE_WORKSPACE")
@ClearEnvironmentVariable(key = "DASHSCOPE_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_HTTP_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_REGION")
@SetEnvironmentVariable(key = "DASHSCOPE_CONFIG", value = "/nonexistent/dashscope-config.toml")
class FilesMockTest {
  private MockWebServer server;
  private DashScopeClient client;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();
    client =
        DashScopeClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .maxRetries(0)
            .build();
  }

  @AfterEach
  void tearDown() throws IOException {
    client.close();
    server.shutdown();
  }

  private static MockResponse okJson(String json) {
    return new MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(json);
  }

  @Test
  void uploadSubmitsMultipartForm() throws Exception {
    Path file = Files.createTempFile("train", ".jsonl");
    try {
      Files.write(file, "{\"a\":1}\n{\"b\":2}\n".getBytes(StandardCharsets.UTF_8));
      server.enqueue(
          okJson(
              "{\"data\":{\"uploaded_files\":[{\"file_id\":\"file-1\",\"name\":\""
                  + file.getFileName()
                  + "\",\"purpose\":\"fine-tune\",\"size\":16,"
                  + "\"uploaded_at\":\"2026-10-22 10:00:00\"}]},\"request_id\":\"r1\"}"));

      FileObject uploaded =
          client
              .files()
              .upload(FileUploadParams.builder().file(file).description("train set").build());

      assertEquals("file-1", uploaded.getFileId());
      assertEquals("fine-tune", uploaded.getPurpose());
      assertEquals(16L, uploaded.getSize());
      assertEquals("2026-10-22 10:00:00", uploaded.getUploadedAt());
      assertEquals("r1", uploaded.getRequestId());

      RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
      assertEquals("POST", request.getMethod());
      assertEquals("/files", request.getPath());
      assertTrue(request.getHeader("Content-Type").startsWith("multipart/form-data"));
      String body = request.getBody().readUtf8();
      assertTrue(body.contains("name=\"purpose\""));
      // Default purpose must reach the wire, not just drive local validation; the temp-file prefix
      // is a neutral "train" so "fine-tune" can only come from the purpose form field.
      assertTrue(body.contains("fine-tune"));
      assertTrue(body.contains("name=\"descriptions\""));
      assertTrue(body.contains("train set"));
      assertTrue(body.contains("name=\"files\"; filename=\"" + file.getFileName() + "\""));
      assertTrue(body.contains("{\"a\":1}"));
    } finally {
      Files.deleteIfExists(file);
    }
  }

  @Test
  void uploadRejectsInvalidJsonlAndMissingFile() throws Exception {
    Path bad = Files.createTempFile("bad", ".jsonl");
    try {
      Files.write(bad, "not json\n".getBytes(StandardCharsets.UTF_8));
      BadRequestException e =
          assertThrows(
              BadRequestException.class,
              () -> client.files().upload(FileUploadParams.builder().file(bad).build()));
      assertTrue(e.getMessage().contains("jsonl"));

      Path missing = bad.resolveSibling("no-such-file.jsonl");
      assertThrows(
          BadRequestException.class,
          () -> client.files().upload(FileUploadParams.builder().file(missing).build()));
      // Local validation: no traffic.
      assertEquals(0, server.getRequestCount());
    } finally {
      Files.deleteIfExists(bad);
    }
  }

  @Test
  void listParsesDataPage() throws Exception {
    server.enqueue(
        okJson(
            "{\"data\":{\"files\":[{\"file_id\":\"f1\",\"purpose\":\"fine-tune\"},"
                + "{\"file_id\":\"f2\"}],\"page_no\":2,\"page_size\":2,\"total\":5},"
                + "\"request_id\":\"r2\"}"));

    FileList page = client.files().list(2, 2);
    assertEquals(2, page.getPageNo());
    assertEquals(5, page.getTotalCount());
    assertEquals(2, page.getFiles().size());
    assertEquals("f1", page.getFiles().get(0).getFileId());
    // Nested copies carry no envelope.
    assertEquals(null, page.getFiles().get(0).getRequestId());
    assertEquals("r2", page.getRequestId());

    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("GET", request.getMethod());
    assertEquals("/files?page_no=2&page_size=2", request.getPath());
  }

  @Test
  void getParsesFile() throws Exception {
    server.enqueue(
        okJson(
            "{\"data\":{\"file_id\":\"f9\",\"name\":\"train.jsonl\",\"size\":128,"
                + "\"upload_time\":\"2026-10-01\"},\"request_id\":\"r3\"}"));
    FileObject file = client.files().get("f9");
    assertEquals("f9", file.getFileId());
    // upload_time alias lands on uploadedAt.
    assertEquals("2026-10-01", file.getUploadedAt());
    assertEquals("r3", file.getRequestId());
    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("/files/f9", request.getPath());
  }

  @Test
  void getNotFoundSurfacesAsBadRequestException() throws Exception {
    // The gateway reports a missing file as 400 InvalidParameter; message text is never sniffed,
    // so the catalog-driven BadRequestException surfaces with the gateway's real status/code.
    server.enqueue(
        new MockResponse()
            .setResponseCode(400)
            .setHeader("Content-Type", "application/json")
            .setBody(
                "{\"code\":\"InvalidParameter\",\"message\":\"File not found.\","
                    + "\"request_id\":\"r4\"}"));
    BadRequestException e =
        assertThrows(BadRequestException.class, () -> client.files().get("gone"));
    assertEquals(400, e.getStatusCode());
    assertEquals("InvalidParameter", e.getCode());
    assertEquals("r4", e.getRequestId());
  }

  @Test
  void deleteReturnsStatusAndValidatesId() throws Exception {
    server.enqueue(okJson("{\"data\":{\"deleted\":true},\"request_id\":\"r5\"}"));
    DeletionStatus status = client.files().delete("f9");
    assertTrue(status.isDeleted());
    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("DELETE", request.getMethod());
    assertEquals("/files/f9", request.getPath());

    assertThrows(IllegalArgumentException.class, () -> client.files().delete("  "));
    assertThrows(IllegalArgumentException.class, () -> client.files().get(null));
  }
}
