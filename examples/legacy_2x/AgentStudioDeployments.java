import com.alibaba.dashscope.agentstudio.AgentStudioClient;
import com.alibaba.dashscope.agentstudio.message.ClientEvents;
import com.alibaba.dashscope.agentstudio.model.Deployment;
import com.alibaba.dashscope.agentstudio.model.DeploymentRun;
import com.alibaba.dashscope.agentstudio.param.DeploymentAgentParam;
import com.alibaba.dashscope.agentstudio.param.DeploymentCreateParam;
import com.alibaba.dashscope.agentstudio.param.DeploymentRunListParam;
import com.alibaba.dashscope.agentstudio.param.DeploymentScheduleParam;
import java.util.Collections;

/**
 * Managed Agent deployment example.
 *
 * <p>Set DASHSCOPE_API_KEY and DASHSCOPE_WORKSPACE before running.
 */
public class AgentStudioDeployments {

  public static void main(String[] args) {
    try (AgentStudioClient client = new AgentStudioClient()) {
      Deployment deployment =
          client
              .deployments()
              .create(
                  DeploymentCreateParam.builder()
                      .name("daily-summary")
                      .description("Generate a daily summary")
                      .agent(DeploymentAgentParam.builder().id("agent_xxx").build())
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
      System.out.printf("deployment: %s %s%n", deployment.getId(), deployment.getStatus());

      DeploymentRun run = client.deployments().run(deployment.getId());
      System.out.printf("run: %s %s%n", run.getId(), run.getStatus());

      for (DeploymentRun item :
          client
              .deployments()
              .listRuns(deployment.getId(), DeploymentRunListParam.builder().limit(20).build())) {
        System.out.printf("%s %s %s%n", item.getId(), item.getTriggerSource(), item.getStatus());
      }
    }
  }
}
