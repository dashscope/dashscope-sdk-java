import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
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
import com.alibaba.dashscope.threads.messages.Messages;
import com.alibaba.dashscope.threads.messages.TextMessageParam;
import com.alibaba.dashscope.threads.messages.ThreadMessage;
import com.alibaba.dashscope.threads.runs.AssistantStreamMessage;
import com.alibaba.dashscope.threads.runs.RequiredAction;
import com.alibaba.dashscope.threads.runs.Run;
import com.alibaba.dashscope.threads.runs.RunParam;
import com.alibaba.dashscope.threads.runs.Runs;
import com.alibaba.dashscope.threads.runs.SubmitToolOutputsParam;
import com.alibaba.dashscope.threads.runs.ThreadAndRunParam;
import com.alibaba.dashscope.threads.runs.ToolOutput;
import com.alibaba.dashscope.tools.FunctionDefinition;
import com.alibaba.dashscope.tools.ToolCallBase;
import com.alibaba.dashscope.tools.wanx.ToolWanX;
import com.alibaba.dashscope.utils.JsonUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.generator.TypeScope;
import com.github.victools.jsonschema.generator.MemberScope;
import io.reactivex.Flowable;
import com.alibaba.dashscope.tools.ToolCallFunction;

public class AssistantFunctionCallStream {

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE})
    public @interface JsonDescription {
        String value();
    }

    private static String resolveJsonDescription(MemberScope<?, ?> member) {
        JsonDescription jsonDescription = member.getAnnotationConsideringFieldAndGetterIfSupported(JsonDescription.class);
        if (jsonDescription != null) {
            return jsonDescription.value();
        }
        return null;
    }
    private static String resolveDescriptionForType(TypeScope scope) {
        Class<?> rawType = scope.getType().getErasedType();
        JsonDescription jsonDescription = rawType.getAnnotation(JsonDescription.class);
        if (jsonDescription != null) {
            return jsonDescription.value();
        }
        return null;
    }


    @JsonDescription("Add two numbers.")
    public class AddFunctionTool {
        @JsonDescription("The left operator")
        private Integer left;
        @JsonDescription("The right operator")
        private Integer right;

        public AddFunctionTool(int left, int right) {
            this.left = left;
            this.right = right;
        }

        public int call() {
            return left + right;
        }
    }

    static ToolFunction buildFunction() {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(
                SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
        configBuilder.forFields().withDescriptionResolver(AssistantFunctionCallStream::resolveJsonDescription);
        configBuilder.forTypesInGeneral().withDescriptionResolver(AssistantFunctionCallStream::resolveDescriptionForType);
        SchemaGeneratorConfig config = configBuilder.with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                .without(Option.FLATTENED_ENUMS_FROM_TOSTRING)
                .build();
        SchemaGenerator generator = new SchemaGenerator(config);

        // generate jsonSchema of function.
        ObjectNode jsonSchema = generator.generateSchema(AddFunctionTool.class);

        // call with tools of function call, jsonSchema.toString() is jsonschema String.
        FunctionDefinition fd = FunctionDefinition.builder().name("add")
                .description("add two number")
                .parameters(JsonUtils.parseString(jsonSchema.toString()).getAsJsonObject()).build();
        return ToolFunction.builder().function(fd).build();
    }

    static public Assistant createAssistant() throws ApiException, NoApiKeyException {
        AssistantParam assistantParam = AssistantParam.builder().model("qwen-max") // model must be
                                                                                   // set.
                .description("a helper assistant").name("system") // name必须填写
                .instructions(
                        "You are a helpful assistant. When asked a question, use tools wherever possible.")
                .tool(buildFunction()).tool(ToolWanX.builder().build()).build();
        Assistants assistants = new Assistants();
        return assistants.create(assistantParam);
    }

    static public void streamRun(String assistantId)
            throws ApiException, NoApiKeyException, InvalidateParameter, InputRequiredException {
        Runs runs = new Runs();
        ThreadParam threadParam = ThreadParam.builder()
                .message(TextMessageParam.builder().role("user")
                        .content("What is transformer? Explain it in simple terms.").build())
                .build();
        ThreadAndRunParam threadAndRunParam =
                ThreadAndRunParam.builder().thread(threadParam).stream(true) // set stream output
                        .assistantId(assistantId).build();

        Flowable<AssistantStreamMessage> streamResponse =
                runs.createStreamThreadAndRun(threadAndRunParam);
        final List<AssistantStreamMessage> assistantStreamMessages = new ArrayList<>();
        streamResponse.blockingForEach(assistantStreamMessage -> {
            System.out.println("Event: " + assistantStreamMessage.getEvent());
            System.out.println("data: ");
            System.out.println(assistantStreamMessage.getData());
            assistantStreamMessages.add(assistantStreamMessage);
        });
        AssistantThread thread = (AssistantThread) assistantStreamMessages.get(0).getData();
        Run run = (Run) assistantStreamMessages.get(assistantStreamMessages.size() - 1).getData();
        // retrieve run
        run = runs.retrieve(thread.getId(), run.getId());
        // list steps
        runs.listSteps(thread.getId(), run.getId(), GeneralListParam.builder().build());

        // create a new message
        TextMessageParam textMessageParam =
                TextMessageParam.builder().role("user").content("Add 87787 to 788988737.").build();
        Messages messages = new Messages();
        ThreadMessage threadMessage = messages.create(thread.getId(), textMessageParam);
        System.out.println(threadMessage);
        RunParam runParam = RunParam.builder().assistantId(assistantId).stream(true).build();
        streamResponse = runs.createStream(thread.getId(), runParam);
        assistantStreamMessages.clear();;
        streamResponse.blockingForEach(assistantStreamMessage -> {
            System.out.println("Event: " + assistantStreamMessage.getEvent());
            System.out.println("data: ");
            System.out.println(assistantStreamMessage.getData());
            assistantStreamMessages.add(assistantStreamMessage);
        });
        run = (Run) assistantStreamMessages.get(assistantStreamMessages.size() - 1).getData();
        if (run.getStatus().equals(Run.Status.REQUIRES_ACTION)) {
            // submit action output.
            RequiredAction requiredAction = run.getRequiredAction();
            if (requiredAction.getType().equals("submit_tool_outputs")) {
                ToolCallBase toolCall = requiredAction.getSubmitToolOutputs().getToolCalls().get(0);
                if (toolCall.getType().equals("function")) {
                    // get function call name and argument, both String.
                    String functionName = ((ToolCallFunction) toolCall).getFunction().getName();
                    String functionArgument =
                            ((ToolCallFunction) toolCall).getFunction().getArguments();
                    if (functionName.equals("add")) {
                        // Create the function object.
                        AddFunctionTool addFunction =
                                JsonUtils.fromJson(functionArgument, AddFunctionTool.class);
                        // call function.
                        int sum = addFunction.call();
                        System.out.println(sum);
                        SubmitToolOutputsParam submitToolOutputsParam =
                                SubmitToolOutputsParam.builder()
                                        .toolOutput(ToolOutput.builder().toolCallId("")
                                                .output(String.valueOf(sum)).build())
                                        .stream(true).build();
                        streamResponse = runs.submitStreamToolOutputs(thread.getId(), run.getId(),
                                submitToolOutputsParam);
                        assistantStreamMessages.clear();
                        streamResponse.blockingForEach(assistantStreamMessage -> {
                            System.out.println("Event: " + assistantStreamMessage.getEvent());
                            System.out.println("data: ");
                            System.out.println(assistantStreamMessage.getData());
                            assistantStreamMessages.add(assistantStreamMessage);
                        });
                    }
                }
            }
        }
        GeneralListParam listParam = GeneralListParam.builder().limit(100l).build();
        ListResult<ThreadMessage> threadMessages = messages.list(thread.getId(), listParam);
        for (ThreadMessage threadMessage2 : threadMessages.getData()) {
            System.out.println(threadMessage2);
        }

    }

    public static void main(String[] args)
            throws ApiException, NoApiKeyException, InputRequiredException, InvalidateParameter {
        Assistant assistant = createAssistant();
        streamRun(assistant.getId());
    }
}
