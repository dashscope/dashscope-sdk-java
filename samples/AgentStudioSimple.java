import com.alibaba.dashscope.agentstudio.AgentStudioClient;
import com.alibaba.dashscope.agentstudio.message.ClientEvents;
import com.alibaba.dashscope.agentstudio.message.ContentBlock;
import com.alibaba.dashscope.agentstudio.message.Message;
import com.alibaba.dashscope.agentstudio.model.Agent;
import com.alibaba.dashscope.agentstudio.model.Session;
import com.alibaba.dashscope.agentstudio.param.AgentCreateParam;
import com.alibaba.dashscope.agentstudio.param.SessionCreateParam;
import com.alibaba.dashscope.agentstudio.resource.AgentStudioEventStream;
import java.util.Collections;

/**
 * AgentStudio quick start: create agent, session, send message, stream reply.
 *
 * <p>Prerequisites: set DASHSCOPE_API_KEY env var, and either DASHSCOPE_WORKSPACE env var or pass
 * workspace to the constructor.
 *
 * <pre>
 * export DASHSCOPE_API_KEY=sk-xxx
 * export DASHSCOPE_WORKSPACE=ws_xxxxxxxxxxxx
 * </pre>
 */
public class AgentStudioSimple {

  public static void main(String[] args) throws Exception {
    // Option 1: apiKey + workspace (production)
    AgentStudioClient client = new AgentStudioClient("sk-xxx", "ws_xxxxxxxxxxxx");

    // Option 2: all from env vars (DASHSCOPE_API_KEY + DASHSCOPE_WORKSPACE)
    // AgentStudioClient client = new AgentStudioClient();

    // Option 3: custom base URL
    // AgentStudioClient client = AgentStudioClient.builder()
    //     .apiKey("sk-xxx")
    //     .baseUrl("https://your-custom-host/api/v1/agentstudio")
    //     .build();

    // 1. Create Agent
    Agent agent =
        client
            .agents()
            .create(
                AgentCreateParam.builder()
                    .name("demo-agent")
                    .model("qwen-plus")
                    .systemPrompt("你是一个简洁的助手。")
                    .build());
    System.out.println("Agent: " + agent.getId());

    // 2. Create Session
    Session session =
        client.sessions().create(SessionCreateParam.builder().agent(agent.getId()).build());
    System.out.println("Session: " + session.getId());

    // 3. Send message
    client
        .sessions()
        .events()
        .send(session.getId(), Collections.singletonList(ClientEvents.userMessage("你好")));

    // 4. Stream reply
    try (AgentStudioEventStream stream = client.sessions().events().stream(session.getId())) {
      for (Message event : stream) {
        if ("message".equals(event.getType()) && event.getContent() != null) {
          for (ContentBlock block : event.getContent()) {
            if (block instanceof ContentBlock.Text) {
              System.out.print(((ContentBlock.Text) block).getText());
            }
          }
        } else if ("session_status".equals(event.getType())) {
          Session.StopReason stopReason = event.getStopReason();
          if (stopReason != null) {
            System.out.println("\nstop_reason: " + stopReason.getType());
          }
          break;
        }
      }
    }

    // Cleanup
    client.sessions().delete(session.getId());
    client.agents().archive(agent.getId());
    client.close();
  }
}
