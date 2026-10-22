// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.atomic.finetunes;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.v3.types.finetunes.FineTune;
import com.alibaba.dashscope.v3.types.finetunes.FineTuneList;

/**
 * Fine-tuning: list jobs and get one job; the full create -> wait -> delete lifecycle is shown
 * commented out. Translation of the Python blueprint example
 * 01_atomic_capabilities/fine_tuning/fine_tunes.py.
 *
 * <p>The read-only list/get sections run as-is. Uncomment the creation section after uploading a
 * training file (see {@code FilesManage}) and setting {@code DASHSCOPE_TRAINING_FILE_ID}.
 */
public final class FineTunesCreate {

  public static void main(String[] args) throws Exception {
    try (DashScopeClient client = ExampleSupport.client()) {
      // List fine-tuning jobs (paged)
      System.out.println("----- list fine-tunes -----");
      FineTuneList page = client.fineTunes().list(1, 10);
      System.out.println("total: " + page.getTotalCount());
      for (FineTune job : page.getJobs()) {
        System.out.println(
            "job_id="
                + job.getJobId()
                + ", status="
                + job.getStatus()
                + ", model="
                + job.getFinetunedModel());
      }

      // Get one job by ID (uses the first job of the list above)
      if (!page.getJobs().isEmpty()) {
        System.out.println("----- get fine-tune -----");
        FineTune job = client.fineTunes().get(page.getJobs().get(0).getJobId());
        System.out.println(
            "job_id="
                + job.getJobId()
                + ", status="
                + job.getStatus()
                + ", finished="
                + job.isFinished());
      }

      // Full lifecycle: create -> wait -> delete. Uncomment after setting
      // DASHSCOPE_TRAINING_FILE_ID to an uploaded file ID.
      //
      // String model = System.getenv().getOrDefault("DASHSCOPE_MODEL", "qwen-turbo");
      // String trainingFileId = System.getenv("DASHSCOPE_TRAINING_FILE_ID");
      //
      // System.out.println("----- create fine-tune -----");
      // FineTune created =
      //     client.fineTunes().create(
      //         com.alibaba.dashscope.v3.types.finetunes.FineTuneCreateParams.builder()
      //             .model(model)
      //             .trainingFileIds(java.util.Collections.singletonList(trainingFileId))
      //             .mode("sft")
      //             .hyperParameters(java.util.Collections.singletonMap("n_epochs", 2))
      //             .finetunedModel("my-finetuned-model")
      //             .build());
      // System.out.println("created job_id=" + created.getJobId());
      //
      // System.out.println("----- wait fine-tune -----");
      // FineTune finalJob =
      //     client.fineTunes().wait(created.getJobId(),
      //         com.alibaba.dashscope.RequestOptions.NONE);
      // System.out.println(
      //     "status=" + finalJob.getStatus() + ", succeeded=" + finalJob.isSucceeded());
      //
      // System.out.println("----- delete fine-tune -----");
      // client.fineTunes().delete(created.getJobId());
    }
  }
}
