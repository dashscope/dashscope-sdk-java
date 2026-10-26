// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.recipes.agentstudio;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.applications.agentstudio.AgentStudioClient;
import com.alibaba.dashscope.applications.agentstudio.message.ClientEvents;
import com.alibaba.dashscope.applications.agentstudio.model.Agent;
import com.alibaba.dashscope.applications.agentstudio.model.Configs;
import com.alibaba.dashscope.applications.agentstudio.model.Environment;
import com.alibaba.dashscope.applications.agentstudio.model.Session;
import com.alibaba.dashscope.applications.agentstudio.param.AgentCreateParam;
import com.alibaba.dashscope.applications.agentstudio.param.EnvironmentCreateParam;
import com.alibaba.dashscope.applications.agentstudio.param.SessionCreateParam;
import com.alibaba.dashscope.applications.agentstudio.resource.AgentStudioEventStream;
import com.alibaba.dashscope.examples.ExampleSupport;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Agent recipe: AgentStudio end-to-end quickstart. Flow: create environment -> create agent ->
 * create session -> send message -> stream events -> archive session.
 *
 * <p>AgentStudio is shared by the 2.x and 3.0 SDK generations: {@code client.agentStudio()} lazily
 * mounts a single shared {@link AgentStudioClient} (workspace and region forward from the client
 * config; the 3.0 {@code baseUrl} does not apply to it), so one {@code DashScopeClient} covers both
 * the 3.0 resources and AgentStudio.
 *
 * <p>The Python example's opt-in {@code memory_stores_demo} flow (create store -> write memory ->
 * list -> update -> list versions -> delete) is already covered by the Java sample {@code
 * legacy_2x/AgentStudioMemoryStores.java}, so it is not duplicated here.
 *
 * <p>Translation of the Python blueprint example 03_agent_recipes/agentstudio.py.
 *
 * <p>Requires DASHSCOPE_API_KEY; DASHSCOPE_WORKSPACE selects the AgentStudio workspace (defaults to
 * trial).
 */
public final class AgentStudioQuickStart {

  private AgentStudioQuickStart() {}

  private static String envOr(String name, String fallback) {
    String value = System.getenv(name);
    return value == null || value.isEmpty() ? fallback : value;
  }

  /** The builtin_toolkit entry from the Python example: everything off except bash. */
  private static Configs.ToolConfig bashOnlyToolkit() {
    Configs.ToolConfig toolkit = new Configs.ToolConfig();
    toolkit.setType("builtin_toolkit");
    Configs.ToolConfig.DefaultConfig defaultConfig = new Configs.ToolConfig.DefaultConfig();
    defaultConfig.setEnabled(false);
    toolkit.setDefaultConfig(defaultConfig);
    Configs.ToolConfig.PerToolConfig bash = new Configs.ToolConfig.PerToolConfig();
    bash.setName("bash");
    bash.setEnabled(true);
    toolkit.setConfigs(Collections.singletonList(bash));
    return toolkit;
  }

  /** Run the full quickstart flow. */
  public static void main(String[] args) {
    try (DashScopeClient client =
        DashScopeClient.builder()
            .apiKey(ExampleSupport.requireApiKey())
            .workspace(envOr("DASHSCOPE_WORKSPACE", "trial"))
            .build()) {
      AgentStudioClient studio = client.agentStudio();

      System.out.println("==> creating environment");
      Map<String, Object> config = new HashMap<>();
      config.put("type", "cloud");
      config.put("networking", Collections.singletonMap("type", "unrestricted"));
      Environment env =
          studio
              .environments()
              .create(
                  EnvironmentCreateParam.builder().name("quickstart-env").config(config).build());
      System.out.println("   environment_id=" + env.getId());

      System.out.println("==> creating agent");
      Agent agent =
          studio
              .agents()
              .create(
                  AgentCreateParam.builder()
                      .name("quickstart-agent")
                      .systemPrompt("You are a friendly demo agent. Reply briefly.")
                      .model("qwen3.8-max")
                      .tools(Collections.singletonList(bashOnlyToolkit()))
                      .build());
      System.out.println("   agent_id=" + agent.getId());

      System.out.println("==> creating session");
      Session session =
          studio
              .sessions()
              .create(
                  SessionCreateParam.builder()
                      .agent(agent.getId())
                      .environmentId(env.getId())
                      .title("quickstart")
                      .build());
      System.out.println("   session_id=" + session.getId());

      System.out.println("==> sending user message");
      studio
          .sessions()
          .events()
          .send(
              session.getId(),
              Collections.singletonList(
                  ClientEvents.userMessage("Hello! Tell me one fun fact about Hangzhou.")));

      System.out.println("==> streaming events");
      try (AgentStudioEventStream stream = studio.sessions().events().stream(session.getId())) {
        for (String text : stream.textStream()) {
          System.out.print(text);
          System.out.flush();
        }
        System.out.println();
      }

      System.out.println("==> archiving session");
      studio.sessions().archive(session.getId());
      System.out.println("done.");
    }
  }
}
