import com.alibaba.dashscope.multimodal.tingwu.TingWuRealtime;
import com.alibaba.dashscope.multimodal.tingwu.TingWuRealtimeCallback;
import com.alibaba.dashscope.multimodal.tingwu.TingWuRealtimeParam;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class TingWuRealtimeUsage {

    public static TingWuRealtimeCallback tingWuRealtimeCallback = new TingWuRealtimeCallback() {

        @Override
        public void onStarted(String taskId) {
            log.debug("onStarted: {}", taskId);
        }

        @Override
        public void onStopped(String taskId) {
            log.debug("onStopped: {}", taskId);
        }

        @Override
        public void onError(String errorCode, String errorMsg) {
            log.debug("onError: {}, {}", errorCode, errorMsg);
        }

        @Override
        public void onAiResult(String taskId, JsonObject content) {
            log.debug("onAiResult: {}, {}", taskId, content.toString());
        }

        @Override
        public void onRecognizeResult(String taskId, JsonObject content) {
            log.debug("onRecognizeResult: {}, {}", taskId, content.toString());
        }

        @Override
        public void onSpeechListen(String taskId, String dataId) {
            log.debug("onSpeechListen: {}", taskId);
        }

        @Override
        public void onClosed() {
            log.debug("onClosed");
        }
    };

    public static void main(String[] args) {
        TingWuRealtimeParam tingWuRealtimeParam = TingWuRealtimeParam.builder()
                .model("tingwu-industrial-instruction")
                .format("pcm")
                .sampleRate(16000)
                .terminology("terminology") //please replace with your terminology
                .appId("your-app-id")
                .apiKey("your-api-key")
                .build();

        Path filePath = Paths.get("local-path/test.pcm");
        // 创建任务
        TingWuRealtime tingWuRealtime = new TingWuRealtime();
        // 启动任务
        tingWuRealtime.call(tingWuRealtimeParam, tingWuRealtimeCallback);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // 发送音频
        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
            // chunk size set to 100 ms for 16KHz sample rate
            byte[] buffer = new byte[3200];
            int bytesRead;
            // Loop to read chunks of the file
            while ((bytesRead = fis.read(buffer)) != -1) {
                ByteBuffer byteBuffer;
                if (bytesRead < buffer.length) {
                    byteBuffer = ByteBuffer.wrap(buffer, 0, bytesRead);
                } else {
                    byteBuffer = ByteBuffer.wrap(buffer);
                }
                // Send the ByteBuffer to the translation and recognition instance
                tingWuRealtime.sendAudioFrame(byteBuffer);
                Thread.sleep(100);
                buffer = new byte[3200];
            }
            tingWuRealtime.stop();
            Thread.sleep(1000 * 10); // wait for 10 seconds
            tingWuRealtime.getDuplexApi().close(1000, "bye");
        } catch (Exception e) {
            e.printStackTrace();
            tingWuRealtime.getDuplexApi().close(1000, "bye");
        }


        System.exit(0);
    }
}
