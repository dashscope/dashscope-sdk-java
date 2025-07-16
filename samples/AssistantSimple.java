import com.alibaba.dashscope.assistants.Assistant;
import com.alibaba.dashscope.assistants.AssistantParam;
import com.alibaba.dashscope.assistants.Assistants;
import com.alibaba.dashscope.common.GeneralListParam;
import com.alibaba.dashscope.common.ListResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.InvalidateParameter;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.threads.AssistantThread;
import com.alibaba.dashscope.threads.ThreadParam;
import com.alibaba.dashscope.threads.Threads;
import com.alibaba.dashscope.threads.messages.Messages;
import com.alibaba.dashscope.threads.messages.TextMessageParam;
import com.alibaba.dashscope.threads.messages.ThreadMessage;
import com.alibaba.dashscope.threads.runs.Run;
import com.alibaba.dashscope.threads.runs.RunParam;
import com.alibaba.dashscope.threads.runs.Runs;

public class AssistantSimple {
    public static void assistantUsage() throws ApiException, NoApiKeyException, InputRequiredException, InvalidateParameter, InterruptedException {
        Assistants assistants = new Assistants();
        // build assistant parameters
        AssistantParam param = AssistantParam.builder()
                // 此处以qwen-max为例，可按需更换模型名称。模型列表：https://help.aliyun.com/zh/model-studio/getting-started/models
                .model("qwen-max")
                .name("intelligent guide")
                .description("a smart guide")
                .instructions("You are a helpful assistant.")
                .build();
        Assistant assistant = assistants.create(param);

        // create a thread
        Threads threads = new Threads();
        AssistantThread assistantThread = threads.create(ThreadParam.builder().build());
        // create a message to thread
        Messages messages = new Messages();
        ThreadMessage message = messages.create(assistantThread.getId(), TextMessageParam.builder().role("user").content("如何做出美味的牛肉炖土豆？").build());
        System.out.println(message);

        // create run
        Runs runs = new Runs();
        RunParam runParam = RunParam.builder().assistantId(assistant.getId()).build();
        Run run = runs.create(assistantThread.getId(), runParam);
        System.out.println(run);

        // wait for run completed
        while(true){
            if(run.getStatus().equals(Run.Status.CANCELLED) ||
                    run.getStatus().equals(Run.Status.COMPLETED) ||
                    run.getStatus().equals(Run.Status.FAILED) ||
                    run.getStatus().equals(Run.Status.REQUIRES_ACTION)||
                    run.getStatus().equals(Run.Status.EXPIRED)){
                break;
            }else{
                Thread.sleep(1000);
            }
            run = runs.retrieve(assistantThread.getId(), run.getId());
            System.out.println(run);
        }

        ListResult<ThreadMessage> threadMessages = messages.list(assistantThread.getId(), GeneralListParam.builder().build());
        System.out.println(threadMessages);
    }

    public static void main(String[] args) throws ApiException, NoApiKeyException, InputRequiredException, InvalidateParameter, InterruptedException {
        assistantUsage();
    }
}