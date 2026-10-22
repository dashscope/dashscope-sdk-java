// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.atomic.files;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.v3.types.files.DeletionStatus;
import com.alibaba.dashscope.v3.types.files.FileList;
import com.alibaba.dashscope.v3.types.files.FileObject;
import com.alibaba.dashscope.v3.types.files.FileUploadParams;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * File management: upload -> list -> get -> delete. Translation of the Python blueprint example
 * 01_atomic_capabilities/files/files.py.
 *
 * <p>Creates a small temporary JSONL file locally so the example is self-contained.
 */
public final class FilesManage {

  public static void main(String[] args) throws Exception {
    // Build a small local file to upload
    Path filePath = Files.createTempFile("dashscope-example", ".jsonl");
    Files.writeString(
        filePath,
        "{\"prompt\": \"你好\", \"completion\": \"你好！有什么可以帮你？\"}\n",
        StandardCharsets.UTF_8);

    try (DashScopeClient client = ExampleSupport.client()) {
      // Upload
      System.out.println("----- upload file -----");
      FileObject uploaded =
          client
              .files()
              .upload(
                  FileUploadParams.builder()
                      .file(filePath)
                      .purpose(FileUploadParams.PURPOSE_FINE_TUNE)
                      .description("example upload")
                      .build());
      System.out.println("file_id: " + uploaded.getFileId() + ", name: " + uploaded.getName());

      // List files
      System.out.println("----- list files -----");
      FileList files = client.files().list(1, 10);
      files.getFiles().stream()
          .limit(5)
          .forEach(
              item ->
                  System.out.println("file_id=" + item.getFileId() + ", name=" + item.getName()));

      // Get one file
      System.out.println("----- get file -----");
      FileObject item = client.files().get(uploaded.getFileId());
      System.out.println("file_id=" + item.getFileId() + ", name=" + item.getName());

      // Delete the uploaded file
      System.out.println("----- delete file -----");
      DeletionStatus deleted = client.files().delete(uploaded.getFileId());
      System.out.println("deleted: " + deleted.isDeleted());
    } finally {
      Files.deleteIfExists(filePath);
    }
  }
}
