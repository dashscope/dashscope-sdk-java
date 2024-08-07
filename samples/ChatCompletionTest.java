import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.alibaba.dashscope.aigc.completion.ChatCompletion;
import com.alibaba.dashscope.aigc.completion.ChatCompletionChunk;
import com.alibaba.dashscope.aigc.completion.ChatCompletionParam;
import com.alibaba.dashscope.aigc.completion.ChatCompletionStreamOptions;
import com.alibaba.dashscope.aigc.completion.ChatCompletions;
import com.alibaba.dashscope.aigc.completion.ChatCompletion.Choice;
import com.alibaba.dashscope.common.ImageURL;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.MessageContentBase;
import com.alibaba.dashscope.common.MessageContentImageURL;
import com.alibaba.dashscope.common.MessageContentText;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.tools.FunctionDefinition;
import com.alibaba.dashscope.tools.ToolCallBase;
import com.alibaba.dashscope.tools.ToolCallFunction;
import com.alibaba.dashscope.tools.ToolFunction;
import com.alibaba.dashscope.utils.JsonUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import io.reactivex.Flowable;

public class ChatCompletionTest {
    public class GetCurrentWeather {
        /** The city and state, e.g. San Francisco, CA */
        private String location;
        /** The temperature unit to use. Infer this from the users location. */
        private String format;

        public GetCurrentWeather(String location, String format) {
            this.location = location;
            this.format = format;
        }

        public String call() {
            float minTemperature = -10.0f;
            float maxTemperature = 60.0f;
            return String.format("The %s, format: %s temperature: %s", location, format,
                    minTemperature + Math.random() * (maxTemperature - minTemperature));
        }
    }
    public class GetNDayWeather {
        private String location;
        private String format;
        Integer numDays;

        public GetNDayWeather(String location, Integer nDays, String format) {
            this.location = location;
            this.format = format;
            this.numDays = nDays;
        }

        public String call() {
            float minTemperature = -10.0f;
            float maxTemperature = 60.0f;
            return String.format("The %s %s days, format: %s temperature: %s", location, numDays,
                    format, minTemperature + Math.random() * (maxTemperature - minTemperature));
        }
    }

    public static void testGeneralRequest()
            throws ApiException, NoApiKeyException, InputRequiredException {
        ChatCompletions chatCompletions = new ChatCompletions();
        Message system =
                Message.builder().content("You are a helpful assistant.").role("system").build();
        Message user = Message.builder().role("user").content("hello").build();
        ChatCompletionParam chatCompletionParam = ChatCompletionParam.builder().logprobs(true)
                .topLogprobs(20).model("gpt-4o").messages(Arrays.asList(system, user)).build();
        ChatCompletion chatCompletion = chatCompletions.call(chatCompletionParam);
        System.out.println(chatCompletion);
    }

    public static void testGeneralRequestStream()
            throws ApiException, NoApiKeyException, InputRequiredException {
        ChatCompletions chatCompletions = new ChatCompletions();
        Message system =
                Message.builder().content("You are a helpful assistant.").role("system").build();
        Message user = Message.builder().role("user").content("hello").build();
        ChatCompletionParam chatCompletionParam = ChatCompletionParam.builder().stream(true)
                .streamOptions(ChatCompletionStreamOptions.builder().includeUsage(true).build())
                .model("gpt-4o").messages(Arrays.asList(system, user)).build();
        Flowable<ChatCompletionChunk> chatCompletionChunks =
                chatCompletions.streamCall(chatCompletionParam);
        chatCompletionChunks.blockingForEach(item -> {
            System.out.println(item);
        });
    }

    public static void testGeneralRequestFunction()
            throws ApiException, NoApiKeyException, InputRequiredException {
        ChatCompletions chatCompletions = new ChatCompletions();
        List<Message> messages = new ArrayList<>();
        Message system = Message.builder().content(
                "Don't make assumptions about what values to plug into functions. Ask for clarification if a user request is ambiguous.")
                .role("system").build();
        Message user =
                Message.builder().role("user").content("What's the weather like today?").build();
        messages.add(system);
        messages.add(user);
        // create jsonschema generator
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(
                SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
        SchemaGeneratorConfig config = configBuilder.with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                .without(Option.FLATTENED_ENUMS_FROM_TOSTRING).build();
        SchemaGenerator generator = new SchemaGenerator(config);

        // generate jsonSchema of function.
        ObjectNode jsonSchema = generator.generateSchema(GetCurrentWeather.class);

        // call with tools of function call, jsonSchema.toString() is jsonschema String.
        FunctionDefinition getCurrentWeatherFunction = FunctionDefinition.builder()
                .name("get_current_weather").description("Get the current weather")
                .parameters(JsonUtils.parseString(jsonSchema.toString()).getAsJsonObject()).build();
        jsonSchema = generator.generateSchema(GetNDayWeather.class);
        FunctionDefinition getNDaysWeatherFunction = FunctionDefinition.builder()
                .name("get_n_day_weather_forecast").description("Get an N-day weather forecast")
                .parameters(JsonUtils.parseString(jsonSchema.toString()).getAsJsonObject()).build();

        ChatCompletionParam chatCompletionParam = ChatCompletionParam.builder().model("gpt-4o")
                .messages(messages)
                .tools(Arrays.asList(
                        ToolFunction.builder().function(getCurrentWeatherFunction).build(),
                        ToolFunction.builder().function(getNDaysWeatherFunction).build()))
                .build();
        ChatCompletion chatCompletion = chatCompletions.call(chatCompletionParam);
        System.out.println(chatCompletion);
        messages.add(chatCompletion.getChoices().get(0).getMessage());
        messages.add(Message.builder().role("user").content("I'm in Glasgow, Scotland.").build());
        chatCompletionParam.setMessages(messages);
        chatCompletion = chatCompletions.call(chatCompletionParam);
        System.out.println(chatCompletion);
        for (Choice choice : chatCompletion.getChoices()) {
            // add the assistant message to list for next Generation call.
            messages.add(choice.getMessage());
            // check if we need call tool.
            if (choice.getMessage().getToolCalls() != null) {
                // iterator the tool calls
                for (ToolCallBase toolCall : choice.getMessage().getToolCalls()) {
                    // get function call.
                    if (toolCall.getType().equals("function")) {
                        // get function call name and argument, both String.
                        String functionName = ((ToolCallFunction) toolCall).getFunction().getName();
                        String functionArgument =
                                ((ToolCallFunction) toolCall).getFunction().getArguments();
                        if (functionName.equals("get_current_weather")) {
                            // Create the function object.
                            GetCurrentWeather fn =
                                    JsonUtils.fromJson(functionArgument, GetCurrentWeather.class);
                            // call function.
                            String weather = fn.call();
                            // create the tool message
                            Message toolResultMessage =
                                    Message.builder().role("tool").content(String.valueOf(weather))
                                            .toolCallId(toolCall.getId()).build();
                            // add the tool message to messages list.
                            messages.add(toolResultMessage);
                        } else if (functionName.equals("get_n_day_weather_forecast")) {
                            // Create the function object.
                            GetNDayWeather fn =
                                    JsonUtils.fromJson(functionArgument, GetNDayWeather.class);
                            // call function.
                            String weather = fn.call();
                            // create the tool message
                            Message toolResultMessage =
                                    Message.builder().role("tool").content(String.valueOf(weather))
                                            .toolCallId(toolCall.getId()).build();
                            // add the tool message to messages list.
                            messages.add(toolResultMessage);
                        }
                    }
                }
            }
        }
        chatCompletionParam.setMessages(messages);
        chatCompletion = chatCompletions.call(chatCompletionParam);
        System.out.println(chatCompletion);
    }

    public static void testGeneralRequestFunctionParallel()
            throws ApiException, NoApiKeyException, InputRequiredException {
        ChatCompletions chatCompletions = new ChatCompletions();
        List<Message> messages = new ArrayList<>();
        Message system = Message.builder().content(
                "Don't make assumptions about what values to plug into functions. Ask for clarification if a user request is ambiguous.")
                .role("system").build();
        Message user = Message.builder().role("user").content("What's the weather like?").build();
        messages.add(system);
        messages.add(user);
        // create jsonschema generator
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(
                SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
        SchemaGeneratorConfig config = configBuilder.with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                .without(Option.FLATTENED_ENUMS_FROM_TOSTRING).build();
        SchemaGenerator generator = new SchemaGenerator(config);

        // generate jsonSchema of function.
        ObjectNode jsonSchema = generator.generateSchema(GetCurrentWeather.class);

        // call with tools of function call, jsonSchema.toString() is jsonschema String.
        FunctionDefinition getCurrentWeatherFunction = FunctionDefinition.builder()
                .name("get_current_weather").description("Get the current weather")
                .parameters(JsonUtils.parseString(jsonSchema.toString()).getAsJsonObject()).build();
        jsonSchema = generator.generateSchema(GetNDayWeather.class);
        FunctionDefinition getNDaysWeatherFunction = FunctionDefinition.builder()
                .name("get_n_day_weather_forecast").description("Get an N-day weather forecast")
                .parameters(JsonUtils.parseString(jsonSchema.toString()).getAsJsonObject()).build();

        ChatCompletionParam chatCompletionParam = ChatCompletionParam.builder().model("gpt-4o")
                .messages(messages)
                .tools(Arrays.asList(
                        ToolFunction.builder().function(getCurrentWeatherFunction).build(),
                        ToolFunction.builder().function(getNDaysWeatherFunction).build()))
                .build();
        ChatCompletion chatCompletion = chatCompletions.call(chatCompletionParam);
        System.out.println(chatCompletion);
        messages.add(chatCompletion.getChoices().get(0).getMessage());
        messages.add(Message.builder().role("user").content(
                "what is the weather going to be like in San Francisco and Glasgow over the next 4 days")
                .build());
        chatCompletionParam.setMessages(messages);
        chatCompletion = chatCompletions.call(chatCompletionParam);
        System.out.println(chatCompletion);
        for (Choice choice : chatCompletion.getChoices()) {
            // add the assistant message to list for next Generation call.
            messages.add(choice.getMessage());
            // check if we need call tool.
            if (choice.getMessage().getToolCalls() != null) {
                // iterator the tool calls
                for (ToolCallBase toolCall : choice.getMessage().getToolCalls()) {
                    // get function call.
                    if (toolCall.getType().equals("function")) {
                        // get function call name and argument, both String.
                        String functionName = ((ToolCallFunction) toolCall).getFunction().getName();
                        String functionArgument =
                                ((ToolCallFunction) toolCall).getFunction().getArguments();
                        if (functionName.equals("get_current_weather")) {
                            // Create the function object.
                            GetCurrentWeather fn =
                                    JsonUtils.fromJson(functionArgument, GetCurrentWeather.class);
                            // call function.
                            String weather = fn.call();
                            // create the tool message
                            Message toolResultMessage =
                                    Message.builder().role("tool").content(String.valueOf(weather))
                                            .toolCallId(toolCall.getId()).build();
                            // add the tool message to messages list.
                            messages.add(toolResultMessage);
                        } else if (functionName.equals("get_n_day_weather_forecast")) {
                            // Create the function object.
                            GetNDayWeather fn =
                                    JsonUtils.fromJson(functionArgument, GetNDayWeather.class);
                            // call function.
                            String weather = fn.call();
                            // create the tool message
                            Message toolResultMessage =
                                    Message.builder().role("tool").content(String.valueOf(weather))
                                            .toolCallId(toolCall.getId()).build();
                            // add the tool message to messages list.
                            messages.add(toolResultMessage);
                        }
                    }
                }
            }
        }
        chatCompletionParam.setMessages(messages);
        chatCompletion = chatCompletions.call(chatCompletionParam);
        System.out.println(chatCompletion);
    }

    public static void testMultiModalRequest()
            throws ApiException, NoApiKeyException, InputRequiredException {
        ChatCompletions chatCompletions = new ChatCompletions();
        Message system =
                Message.builder().content("You are a helpful assistant.").role("system").build();
        List<MessageContentBase> messageContents = Arrays.asList(
                MessageContentText.builder().text("What’s in this image?").build(),
                MessageContentImageURL.builder().imageURL(ImageURL.builder().url(
                        "https://upload.wikimedia.org/wikipedia/commons/thumb/d/dd/Gfp-wisconsin-madison-the-nature-boardwalk.jpg/2560px-Gfp-wisconsin-madison-the-nature-boardwalk.jpg")
                        .detail("high").build()).build());
        Message user = Message.builder().role("user").contents(messageContents).build();
        ChatCompletionParam chatCompletionParam = ChatCompletionParam.builder().model("gpt-4o")
                .messages(Arrays.asList(system, user)).build();
        ChatCompletion chatCompletion = chatCompletions.call(chatCompletionParam);
        System.out.println(chatCompletion);
    }

    public static void main(String[] args) {
        try {
            // testGeneralRequest();
            // testGeneralRequestStream();
            // testGeneralRequestFunctionParallel();
            testMultiModalRequest();
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            System.out.println(e.getMessage());
        }
        System.exit(0);
    }
}
