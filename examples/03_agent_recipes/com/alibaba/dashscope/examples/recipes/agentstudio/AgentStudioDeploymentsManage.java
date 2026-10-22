// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.recipes.agentstudio;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.agentstudio.AgentStudioClient;
import com.alibaba.dashscope.agentstudio.message.ClientEvents;
import com.alibaba.dashscope.agentstudio.model.Deployment;
import com.alibaba.dashscope.agentstudio.model.DeploymentRun;
import com.alibaba.dashscope.agentstudio.param.DeploymentAgentParam;
import com.alibaba.dashscope.agentstudio.param.DeploymentCreateParam;
import com.alibaba.dashscope.agentstudio.param.DeploymentRunListParam;
import com.alibaba.dashscope.agentstudio.param.DeploymentScheduleParam;
import com.alibaba.dashscope.examples.ExampleSupport;
import java.util.Collections;

/**
 * Agent recipe: create and run a Managed Agent deployment.
 *
 * <p>A deployment wraps an agent with a cron schedule and a bootstrap event list, so the agent runs
 * unattended (here: weekday mornings, Asia/Shanghai). The flow: create deployment -> trigger one
 * manual run -> list recent runs.
 *
 * <p>AgentStudio is shared by the 2.x and 3.0 SDK generations: {@code client.agentStudio()} lazily
 * mounts a single shared {@link AgentStudioClient} (workspace and region forward from the client
 * config), so one {@code DashScopeClient} covers both the 3.0 resources and AgentStudio.
 *
 * <p>Translation of the Python blueprint example 03_agent_recipes/agentstudio_deployments.py.
 *
 * <p>Requires DASHSCOPE_API_KEY; DASHSCOPE_WORKSPACE selects the AgentStudio workspace (defaults to
 * trial); DASHSCOPE_AGENT_ID must name an existing agent (see {@code AgentStudioQuickStart}).
 */
public final class AgentStudioDeploymentsManage {

  /** Placeholder, as in the Python blueprint: point DASHSCOPE_AGENT_ID at a real agent. */
  private static final String AGENT_ID = envOr("DASHSCOPE_AGENT_ID", "agent_xxx");

  private AgentStudioDeploymentsManage() {}

  private static String envOr(String name, String fallback) {
    String value = System.getenv(name);
    return value == null || value.isEmpty() ? fallback : value;
  }

  /** Create the deployment, fire one manual run, then list recent runs. */
  public static void main(String[] args) {
    try (DashScopeClient client =
        DashScopeClient.builder()
            .apiKey(ExampleSupport.requireApiKey())
            .workspace(envOr("DASHSCOPE_WORKSPACE", "trial"))
            .build()) {
      AgentStudioClient studio = client.agentStudio();

      Deployment deployment =
          studio
              .deployments()
              .create(
                  DeploymentCreateParam.builder()
                      .name("daily-summary")
                      .description("Generate a daily summary")
                      .agent(DeploymentAgentParam.builder().id(AGENT_ID).build())
                      .schedule(
                          DeploymentScheduleParam.builder()
                              .type(DeploymentScheduleParam.TYPE_CRON)
                              .expression("0 9 * * 1-5")
                              .timezone("Asia/Shanghai")
                              .build())
                      .initialEvents(
                          Collections.singletonList(
                              ClientEvents.userMessage("Summarize yesterday's orders")))
                      .metadata(Collections.singletonMap("biz", "summary"))
                      .build());
      System.out.println("deployment: " + deployment.getId() + " " + deployment.getStatus());

      DeploymentRun run = studio.deployments().run(deployment.getId());
      System.out.println("run: " + run.getId() + " " + run.getStatus());

      for (DeploymentRun item :
          studio
              .deployments()
              .listRuns(deployment.getId(), DeploymentRunListParam.builder().limit(20).build())) {
        System.out.println(item.getId() + " " + item.getTriggerSource() + " " + item.getStatus());
      }
    }
  }
}
