// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.atomic.deployments;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.types.deployments.Deployment;
import com.alibaba.dashscope.types.deployments.DeploymentList;

/**
 * Model deployments: list deployed models and get one; the billed create -> scale -> delete
 * lifecycle is shown commented out (same convention as {@code FineTunesCreate}).
 *
 * <p>The read-only list/get sections run as-is. Uncomment the lifecycle section to deploy a
 * fine-tuned model (see {@code FineTunesCreate}); capacity is billed while the deployment runs.
 * This is the model-deployment service {@code client.deployments()}, not the AgentStudio
 * scheduled-deployment service in {@code recipes.agentstudio.AgentStudioDeploymentsManage}.
 */
public final class DeploymentsManage {

  public static void main(String[] args) throws Exception {
    try (DashScopeClient client = ExampleSupport.client()) {
      // List deployments (paged)
      System.out.println("----- list deployments -----");
      DeploymentList page = client.deployments().list(1, 10);
      System.out.println("total: " + page.getTotalCount());
      for (Deployment deployment : page.getDeployments()) {
        System.out.println(
            "deployed_model="
                + deployment.getDeployedModel()
                + ", status="
                + deployment.getStatus()
                + ", capacity="
                + deployment.getCapacity());
      }

      // Get one deployment by deployed_model (uses the first entry of the list above)
      if (!page.getDeployments().isEmpty()) {
        System.out.println("----- get deployment -----");
        Deployment deployment =
            client.deployments().get(page.getDeployments().get(0).getDeployedModel());
        System.out.println(
            "deployed_model="
                + deployment.getDeployedModel()
                + ", status="
                + deployment.getStatus()
                + ", ready_capacity="
                + deployment.getReadyCapacity());
      }

      // Full lifecycle: create -> scale -> delete. Uncomment to deploy a fine-tuned model;
      // capacity is billed until the deployment is deleted.
      //
      // System.out.println("----- create deployment -----");
      // Deployment created =
      //     client.deployments().create(
      //         com.alibaba.dashscope.types.deployments.DeploymentCreateParams.builder()
      //             .model("my-finetuned-model")
      //             .capacity(1)
      //             .build());
      // System.out.println("created deployed_model=" + created.getDeployedModel());
      //
      // System.out.println("----- scale deployment -----");
      // Deployment scaled = client.deployments().scale(created.getDeployedModel(), 2);
      // System.out.println("status=" + scaled.getStatus() + ", capacity=" + scaled.getCapacity());
      //
      // System.out.println("----- delete deployment -----");
      // com.alibaba.dashscope.types.common.JobStatus deleted =
      //     client.deployments().delete(created.getDeployedModel());
      // System.out.println("status=" + deleted.getStatus());
    }
  }
}