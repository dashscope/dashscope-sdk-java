import com.alibaba.dashscope.assistants.Assistant;
import com.alibaba.dashscope.assistants.AssistantParam;
import com.alibaba.dashscope.tools.search.ToolQuarkSearch;
import io.reactivex.Flowable;
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
import com.alibaba.dashscope.threads.runs.AssistantStreamMessage;
import com.alibaba.dashscope.threads.runs.RunParam;
import com.alibaba.dashscope.threads.runs.Runs;

public class AssistantCallSearchStream {
    static public Assistant createAssistant() throws ApiException, NoApiKeyException{
        AssistantParam assistantParam = AssistantParam.builder()
        .model("qwen-max") // model must be set.
        .description("a helper assistant")
        .name("system")  // name必须填写
        .instructions("You are a helpful assistant. When asked a question, use tools wherever possible.")
        .tool(ToolQuarkSearch.builder().build())
        .build();
        Assistants assistants = new Assistants();
        return assistants.create(assistantParam);
    }

    static public void run(String assistantId) throws ApiException, NoApiKeyException, InvalidateParameter, InputRequiredException, InterruptedException{
        // create a thread
        Threads threads = new Threads();
        AssistantThread assistantThread = threads.create(ThreadParam.builder().build());    
        
        Runs runs = new Runs();
        // create a new message
        TextMessageParam textMessageParam = TextMessageParam.builder().role("user").content("请帮忙查询今日北京天气？").build();
        Messages messages = new Messages();
        ThreadMessage threadMessage = messages.create(assistantThread.getId(), textMessageParam);
        System.out.println(threadMessage);
        // set stream to true
        RunParam runParam = RunParam.builder().assistantId(assistantId).stream(true).build();
        Flowable<AssistantStreamMessage> runFlowable = runs.createStream(assistantThread.getId(), runParam);
        runFlowable.blockingForEach(assistantStreamMessage->{
            System.out.println("Event: " + assistantStreamMessage.getEvent());
            System.out.println("data: ");
            System.out.println(assistantStreamMessage.getData());
        });

        GeneralListParam listParam = GeneralListParam.builder().limit(100l).build();
        ListResult<ThreadMessage> threadMessages = messages.list(assistantThread.getId(), listParam);
        for(ThreadMessage threadMessage2: threadMessages.getData()){
            System.out.println(threadMessage2);
        }

    }

    public static void main(String[] args) throws ApiException, NoApiKeyException, InputRequiredException, InvalidateParameter, InterruptedException {
        Assistant assistant = createAssistant();
        run(assistant.getId());
        System.exit(0);
    }
}
