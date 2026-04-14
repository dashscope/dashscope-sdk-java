import com.alibaba.dashscope.audio.omni.*;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Qwen3 Omni Tool Calling Support
 * 
 * This example demonstrates:
 * 1. Function calling (tool) support with weather and flight price queries
 * 2. Using createItem to send tool call results back to the server
 */
public class Qwen3OmniToolCallUsage {
    private static final Logger log = LoggerFactory.getLogger(Qwen3OmniToolCallUsage.class);
    private static final int AUDIO_CHUNK_SIZE = 3200; // Audio chunk size in bytes (200ms at 16kHz)
    private static final int SLEEP_INTERVAL_MS = 200; // Sleep interval to simulate real-time streaming

    // Store pending tool calls that need response
    private static final Map<String, JsonObject> pendingToolCalls = new ConcurrentHashMap<>();

    public static void main(String[] args) throws InterruptedException {
        // Build connection parameters
        OmniRealtimeParam param = OmniRealtimeParam.builder()
                .model("model-name") // Replace with your model
                .apikey("your-api-key")
                .url("wss://dashscope.aliyuncs.com/api-ws/v1/realtime") // Custom URL if needed
                .build();

        final AtomicReference<StringBuilder> responseTextRef = new AtomicReference<>(new StringBuilder());
        final CountDownLatch finishLatch = new CountDownLatch(1);

        // Create conversation with callback
        OmniRealtimeConversation conversation = new OmniRealtimeConversation(param, new OmniRealtimeCallback() {
            private long lastPackageTime = 0;
            private boolean isFirstText = true;
            private boolean isFirstAudio = true;

            @Override
            public void onOpen() {
                System.out.println("connection opened, ready to send audio");
                lastPackageTime = System.currentTimeMillis();
            }

            @Override
            public void onEvent(JsonObject message) {
                String type = message.get("type").getAsString();

                switch (type) {
                    case "session.created":
                        System.out.println("start session: " + message.get("session").getAsJsonObject().get("id").getAsString());
                        break;

                    case "conversation.item.input_audio_transcription.completed":
                        System.out.println("question: " + message.get("transcript").getAsString());
                        break;

                    case "response.audio_transcript.delta":
                    case "response.text.delta":
                        if (isFirstText) {
                            isFirstText = false;
                            System.out.println("first text latency from vad end: " + (System.currentTimeMillis() - lastPackageTime) + " ms");
                        }
                        String text = message.get("delta").getAsString();
                        responseTextRef.get().append(text);
                        break;

                    case "response.audio.delta":
                        if (isFirstAudio) {
                            isFirstAudio = false;
                            System.out.println("first audio latency from vad end: " + (System.currentTimeMillis() - lastPackageTime) + " ms");
                        }
                        System.out.println("audio interval: " + (System.currentTimeMillis() - lastPackageTime) + " ms");
                        lastPackageTime = System.currentTimeMillis();
                        String recvAudioB64 = message.get("delta").getAsString();
                        // Handle received audio - implement your own audio player here
                        // audioPlayer.write(recvAudioB64);
                        break;

                    case "input_audio_buffer.speech_started":
                        System.out.println("======VAD Speech Start======");
                        // Cancel audio playback when user starts speaking
                        // audioPlayer.cancelPlaying();
                        break;

                    case "input_audio_buffer.speech_stopped":
                        System.out.println("======VAD Speech End======");
                        lastPackageTime = System.currentTimeMillis();
                        isFirstText = true;
                        isFirstAudio = true;
                        pendingToolCalls.clear();
                        break;

                    case "response.function_call_arguments.done":
                        System.out.println("======TOOL CALL======");
                        String toolCallId = message.get("call_id").getAsString();
                        pendingToolCalls.put(toolCallId, message);
                        break;

                    case "response.done":
                        System.out.println("======RESPONSE DONE======");
                        System.out.println("all response text: " + responseTextRef.get());
                        responseTextRef.set(new StringBuilder()); // Clear for next response
                        break;

                    default:
                        break;
                }
            }

            @Override
            public void onClose(int code, String reason) {
                System.out.println("connection closed with code: " + code + ", reason: " + reason);
                finishLatch.countDown();
            }
        });

        try {
            conversation.connect();
        } catch (NoApiKeyException e) {
            throw new RuntimeException(e);
        }

        // Build tools definition
        List<Map<String, Object>> tools = buildTools();

        // Configure session with tools and server VAD
        Map<String, Object> extraParams = new HashMap<>();
        extraParams.put("tools", tools);

        OmniRealtimeConfig config = OmniRealtimeConfig.builder()
                .modalities(Arrays.asList(OmniRealtimeModality.AUDIO, OmniRealtimeModality.TEXT))
                .voice("Ethan") // Voice name
                .inputAudioFormat(OmniRealtimeAudioFormat.PCM_16000HZ_MONO_16BIT)
                .outputAudioFormat(OmniRealtimeAudioFormat.PCM_24000HZ_MONO_16BIT)
                .enableInputAudioTranscription(true)
                .InputAudioTranscription("gummy-realtime-v1") // Transcription model
                .enableTurnDetection(true)
                .turnDetectionType("server_vad")
                .parameters(extraParams) // Pass tools through extra parameters
                .build();

        conversation.updateSession(config);

        System.out.println("Press 'Ctrl+C' to stop conversation...");

        // Main loop - read audio from file and send to server
        // In a real application, you would read from microphone
        String filePath = "./weather.wav";
        File audioFile = new File(filePath);

        if (!audioFile.exists()) {
            log.error("Audio file not found: {}", filePath);
            System.out.println("Waiting for interactive session. Press Ctrl+C to exit.");
            // For demo purposes, just wait
            finishLatch.await();
            return;
        }

        try (FileInputStream audioInputStream = new FileInputStream(audioFile)) {
            byte[] audioBuffer = new byte[AUDIO_CHUNK_SIZE];
            int bytesRead;

            log.info("Starting to send audio data from: {}", filePath);

            while ((bytesRead = audioInputStream.read(audioBuffer)) != -1) {
                // Check and handle pending tool calls
                boolean needResponse = handlePendingToolCalls(conversation);

                if (needResponse) {
                    System.out.println("*** create response after call tools");
                    conversation.createResponse(null, Arrays.asList(OmniRealtimeModality.AUDIO, OmniRealtimeModality.TEXT));
                    System.out.println("======TOOL CALL END======");
                }

                // Send audio data
                String audioB64 = Base64.getEncoder().encodeToString(Arrays.copyOf(audioBuffer, bytesRead));
                conversation.appendAudio(audioB64);

                // Add small delay to simulate real-time audio streaming
                Thread.sleep(SLEEP_INTERVAL_MS);
            }

            log.info("Finished sending audio data.");

        } catch (Exception e) {
            log.error("Error sending audio from file: {}", filePath, e);
        }
        //wait 5 seconds for demo response done
        Thread.sleep(5 * 1000);
        conversation.close(1000, "bye");
        finishLatch.await();
        System.exit(0);
    }

    /**
     * Build tool definitions in OpenAI format
     */
    private static List<Map<String, Object>> buildTools() {
        List<Map<String, Object>> tools = new ArrayList<>();

        // Tool: get_current_weather
        Map<String, Object> weatherTool = new HashMap<>();
        weatherTool.put("type", "function");
        Map<String, Object> weatherFunction = new HashMap<>();
        weatherFunction.put("name", "get_current_weather");
        weatherFunction.put("description", "当你想查询指定城市的天气时非常有用。");
        Map<String, Object> weatherParams = new HashMap<>();
        weatherParams.put("type", "object");
        Map<String, Object> locationProp = new HashMap<>();
        locationProp.put("type", "string");
        locationProp.put("description", "城市或县区，比如北京市、杭州市、余杭区等。");
        Map<String, Object> weatherProps = new HashMap<>();
        weatherProps.put("location", locationProp);
        weatherParams.put("properties", weatherProps);
        weatherParams.put("required", Collections.singletonList("location"));
        weatherFunction.put("parameters", weatherParams);
        weatherTool.put("function", weatherFunction);
        tools.add(weatherTool);

        // Tool: get_flight_price
        Map<String, Object> flightTool = new HashMap<>();
        flightTool.put("type", "function");
        Map<String, Object> flightFunction = new HashMap<>();
        flightFunction.put("name", "get_flight_price");
        flightFunction.put("description", "当你想查询飞机票价格时非常有用。");
        Map<String, Object> flightParams = new HashMap<>();
        flightParams.put("type", "object");
        Map<String, Object> srcProp = new HashMap<>();
        srcProp.put("type", "string");
        srcProp.put("description", "飞机起飞的城市，比如北京市、杭州市等。");
        Map<String, Object> dstProp = new HashMap<>();
        dstProp.put("type", "string");
        dstProp.put("description", "飞机降落的城市，比如北京市、杭州市区等。");
        Map<String, Object> flightProps = new HashMap<>();
        flightProps.put("src", srcProp);
        flightProps.put("dst", dstProp);
        flightParams.put("properties", flightProps);
        flightParams.put("required", Arrays.asList("src", "dst"));
        flightFunction.put("parameters", flightParams);
        flightTool.put("function", flightFunction);
        tools.add(flightTool);

        // Tool: get_train_price
        Map<String, Object> trainTool = new HashMap<>();
        trainTool.put("type", "function");
        Map<String, Object> trainFunction = new HashMap<>();
        trainFunction.put("name", "get_train_price");
        trainFunction.put("description", "当你想查询火车票价格时非常有用。");
        Map<String, Object> trainParams = new HashMap<>();
        trainParams.put("type", "object");
        Map<String, Object> trainSrcProp = new HashMap<>();
        trainSrcProp.put("type", "string");
        trainSrcProp.put("description", "火车出发的城市，比如北京市、杭州市等。");
        Map<String, Object> trainDstProp = new HashMap<>();
        trainDstProp.put("type", "string");
        trainDstProp.put("description", "火车到达的城市，比如北京市、杭州市区等。");
        Map<String, Object> trainProps = new HashMap<>();
        trainProps.put("src", trainSrcProp);
        trainProps.put("dst", trainDstProp);
        trainParams.put("properties", trainProps);
        trainParams.put("required", Arrays.asList("src", "dst"));
        trainFunction.put("parameters", trainParams);
        trainTool.put("function", trainFunction);
        tools.add(trainTool);

        return tools;
    }

    /**
     * Handle pending tool calls by executing local functions and sending results back
     */
    private static boolean handlePendingToolCalls(OmniRealtimeConversation conversation) {
        boolean needResponse = false;

        for (Map.Entry<String, JsonObject> entry : pendingToolCalls.entrySet()) {
            JsonObject toolCallResponse = entry.getValue();

            // Process tool call
            JsonObject result = handleToolCall(toolCallResponse);

            // Send result back using createItem
            sendToolCallResult(conversation, result);

            needResponse = true;
            pendingToolCalls.remove(entry.getKey());
        }

        return needResponse;
    }

    /**
     * Handle a single tool call and return the result
     */
    private static JsonObject handleToolCall(JsonObject toolCallResponse) {
        String functionName = toolCallResponse.get("name").getAsString();
        JsonObject arguments = new Gson().fromJson(toolCallResponse.get("arguments").getAsString(), JsonObject.class);

        System.out.println("[Tool Call] start handling tool call: name: " + functionName + ", args: " + arguments);

        String output;
        switch (functionName) {
            case "get_current_weather":
                String location = arguments.get("location").getAsString();
                output = getCurrentWeather(location);
                break;
            case "get_flight_price":
                String src = arguments.get("src").getAsString();
                String dst = arguments.get("dst").getAsString();
                output = getFlightPrice(src, dst);
                break;
            case "get_train_price":
                String trainSrc = arguments.get("src").getAsString();
                String trainDst = arguments.get("dst").getAsString();
                output = getTrainPrice(trainSrc, trainDst);
                break;
            default:
                output = "client没有找到这个工具，调用失败。";
                break;
        }

        System.out.println("[Tool Call] tool call response: " + output);

        // Build result object
        JsonObject result = new JsonObject();
        result.addProperty("call_id", toolCallResponse.get("call_id").getAsString());
        result.addProperty("output", output);
        return result;
    }

    /**
     * Send tool call result back to server using createItem
     */
    private static void sendToolCallResult(OmniRealtimeConversation conversation, JsonObject result) {
        JsonObject item = new JsonObject();
        item.addProperty("id", "item_" + UUID.randomUUID().toString().replace("-", ""));
        item.addProperty("type", "function_call_output");
        item.addProperty("call_id", result.get("call_id").getAsString());
        item.addProperty("output", result.get("output").getAsString());

        conversation.createItem(item);
    }

    // ===== Local tool implementations =====

    private static String getCurrentWeather(String location) {
        return location + "今天天气为霾转晴，气温4/-4℃，微风";
    }

    private static String getFlightPrice(String src, String dst) {
        return src + "到" + dst + "的机票价格为200~300美元。";
    }

    private static String getTrainPrice(String src, String dst) {
        return "invalid apikey error";
    }
}
