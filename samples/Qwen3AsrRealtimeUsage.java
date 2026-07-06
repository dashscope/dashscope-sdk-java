import com.alibaba.dashscope.audio.omni.*;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.LineUnavailableException;
import java.io.File;
import java.io.FileInputStream;
import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;


public class Qwen3AsrRealtimeUsage {
    private static final Logger log = LoggerFactory.getLogger(Qwen3AsrRealtimeUsage.class);
    private static final int AUDIO_CHUNK_SIZE = 1024; // Audio chunk size in bytes
    private static final int SLEEP_INTERVAL_MS = 30;  // Sleep interval in milliseconds

    public static void main(String[] args) throws InterruptedException, LineUnavailableException {

        OmniRealtimeParam param = OmniRealtimeParam.builder()
                .model("qwen3-asr-flash-realtime")
                .apikey(System.getenv("DASHSCOPE_API_KEY"))
                .build();

        OmniRealtimeConversation conversation = null;
        final AtomicReference<OmniRealtimeConversation> conversationRef = new AtomicReference<>(null);
        conversation = new OmniRealtimeConversation(param, new OmniRealtimeCallback() {
            @Override
            public void onOpen() {
                System.out.println("connection opened");
            }
            @Override
            public void onEvent(JsonObject message) {
                String type = message.get("type").getAsString();
                switch(type) {
                    case "session.created":
                        System.out.println("start session: " + message.get("session").getAsJsonObject().get("id").getAsString());
                        break;
                    case "conversation.item.input_audio_transcription.completed":
                        System.out.println("question: " + message.get("transcript").getAsString());
                        break;
                    case "response.audio_transcript.delta":
                        System.out.println("got llm response delta: " + message.get("delta").getAsString());
                        break;
                    case "input_audio_buffer.speech_started":
                        System.out.println("======VAD Speech Start======");
                        break;
                    case "input_audio_buffer.speech_stopped":
                        System.out.println("======VAD Speech Stop======");
                        break;
                    case "response.done":
                        System.out.println("======RESPONSE DONE======");
                        if (conversationRef.get() != null) {
                            System.out.println("[Metric] response: " + conversationRef.get().getResponseId() +
                                    ", first text delay: " + conversationRef.get().getFirstTextDelay() +
                                    " ms, first audio delay: " + conversationRef.get().getFirstAudioDelay() + " ms");
                        }
                        break;
                    default:
                        break;
                }
            }
            @Override
            public void onClose(int code, String reason) {
                System.out.println("connection closed code: " + code + ", reason: " + reason);
            }
        });
        conversationRef.set(conversation);
        try {
            conversation.connect();
        } catch (NoApiKeyException e) {
            throw new RuntimeException(e);
        }


        OmniRealtimeTranscriptionParam transcriptionParam = new OmniRealtimeTranscriptionParam();
        transcriptionParam.setLanguage("zh");
        transcriptionParam.setInputAudioFormat("pcm");
        transcriptionParam.setInputSampleRate(16000);
        transcriptionParam.setCorpusText("这是一段脱口秀表演");

        OmniRealtimeConfig config = OmniRealtimeConfig.builder()
                .modalities(Collections.singletonList(OmniRealtimeModality.TEXT))
                .transcriptionConfig(transcriptionParam)
                .build();
        conversation.updateSession(config);


        String filePath = "./path/to/your/audio/16k-16bit-mono-file.pcm";
        File audioFile = new File(filePath);

        if (!audioFile.exists()) {
            log.error("Audio file not found: {}", filePath);
            return;
        }

        try (FileInputStream audioInputStream = new FileInputStream(audioFile)) {
            byte[] audioBuffer = new byte[AUDIO_CHUNK_SIZE];
            int bytesRead;
            int totalBytesRead = 0;

            log.info("Starting to send audio data from: {}", filePath);

            // Read and send audio data in chunks
            while ((bytesRead = audioInputStream.read(audioBuffer)) != -1) {
                totalBytesRead += bytesRead;
                String audioB64 = Base64.getEncoder().encodeToString(audioBuffer);
                // Send audio chunk to conversation
                conversation.appendAudio(audioB64);

                // Add small delay to simulate real-time audio streaming
                Thread.sleep(SLEEP_INTERVAL_MS);
            }

            log.info("Finished sending audio data. Total bytes sent: {}", totalBytesRead);

        } catch (Exception e) {
            log.error("Error sending audio from file: {}", filePath, e);
        }

        conversation.commit();
        conversation.createResponse(null, null);
        conversation.close(1000, "bye");
        System.exit(0);
    }
}
