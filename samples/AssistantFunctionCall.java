import com.alibaba.dashscope.assistants.Assistant;
import com.alibaba.dashscope.assistants.AssistantParam;
import com.alibaba.dashscope.tools.ToolFunction;
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
import com.alibaba.dashscope.threads.runs.RequiredAction;
import com.alibaba.dashscope.threads.runs.Run;
import com.alibaba.dashscope.threads.runs.RunParam;
import com.alibaba.dashscope.threads.runs.Runs;
import com.alibaba.dashscope.threads.runs.SubmitToolOutputsParam;
import com.alibaba.dashscope.threads.runs.ToolOutput;
import com.alibaba.dashscope.tools.FunctionDefinition;
import com.alibaba.dashscope.tools.ToolBase;
import com.alibaba.dashscope.tools.ToolCallBase;
import com.alibaba.dashscope.utils.JsonUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.alibaba.dashscope.tools.ToolCallFunction;

public class AssistantFunctionCall {
    public class AddFunctionTool {
        private int left;
        private int right;

        public AddFunctionTool(int left, int right) {
            this.left = left;
            this.right = right;
        }

        public int call() {
            return left + right;
        }
    }

    static ToolFunction buildFunction() {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12,
                OptionPreset.PLAIN_JSON);
        SchemaGeneratorConfig config = configBuilder.with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                .without(Option.FLATTENED_ENUMS_FROM_TOSTRING).build();
        SchemaGenerator generator = new SchemaGenerator(config);

        // generate jsonSchema of function.
        ObjectNode jsonSchema = generator.generateSchema(AddFunctionTool.class);

        // call with tools of function call, jsonSchema.toString() is jsonschema String.
        FunctionDefinition fd = FunctionDefinition.builder().name("add").description("add two number")
                .parameters(JsonUtils.parseString(jsonSchema.toString()).getAsJsonObject()).build();
        return ToolFunction.builder().function(fd).build();
    }
    static public Assistant createAssistant() throws ApiException, NoApiKeyException{
        AssistantParam assistantParam = AssistantParam.builder()
        .model("qwen-max") // model must be set.
        .description("a helper assistant")
        .name("system")  // name必须填写
        .instructions("You are a helpful assistant. When asked a question, use tools wherever possible.")
        .tool(buildFunction())
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
        TextMessageParam textMessageParam = TextMessageParam.builder().role("user").content("Add 87787 to 788988737.").build();
        Messages messages = new Messages();
        ThreadMessage threadMessage = messages.create(assistantThread.getId(), textMessageParam);
        System.out.println(threadMessage);
        RunParam runParam = RunParam.builder().assistantId(assistantId).build();
        Run run = runs.create(assistantThread.getId(), runParam);
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
        }
        if(run.getStatus().equals(Run.Status.REQUIRES_ACTION)){   
            // submit action output.
            RequiredAction requiredAction = run.getRequiredAction();
            if(requiredAction.getType().equals("submit_tool_outputs")){
                ToolCallBase toolCall = requiredAction.getSubmitToolOutputs().getToolCalls().get(0);
                if (toolCall.getType().equals("function")) {
                    // get function call name and argument, both String.
                    String functionName = ((ToolCallFunction) toolCall).getFunction().getName();
                    String functionId = ((ToolCallFunction)toolCall).getId();
                    String functionArgument = ((ToolCallFunction) toolCall).getFunction().getArguments();
                    if (functionName.equals("add")) {
                      // Create the function object.
                      AddFunctionTool addFunction =
                          JsonUtils.fromJson(functionArgument, AddFunctionTool.class);
                      // call function.
                      int sum = addFunction.call();
                      System.out.println(sum);
                      SubmitToolOutputsParam submitToolOutputsParam = SubmitToolOutputsParam.builder()
                      .toolOutput(ToolOutput.builder().toolCallId(functionId).output(String.valueOf(sum)).build())
                      .build();
                      run = runs.submitToolOutputs(assistantThread.getId(), run.getId(), submitToolOutputsParam);
                    }
                  }
            }    
        }
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
        }        

        GeneralListParam listParam = GeneralListParam.builder().limit(100l).build();
        ListResult<ThreadMessage> threadMessages = messages.list(assistantThread.getId(), listParam);
        for(ThreadMessage threadMessage2: threadMessages.getData()){
            System.out.println(threadMessage2);
        }

    }

    public static void main(String[] args) throws ApiException, NoApiKeyException, InputRequiredException, InvalidateParameter, InterruptedException {
        Assistant assistant = createAssistant();
        run(assistant.getId());
    }
}