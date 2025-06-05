import com.alibaba.dashscope.multimodal.MultiModalDialog;
import com.alibaba.dashscope.multimodal.State;
import com.alibaba.dashscope.multimodal.MultiModalDialogCallback;
import com.alibaba.dashscope.multimodal.MultiModalRequestParam;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import static java.lang.Thread.sleep;
/**
 * @author songsong.shao
 * @date 2025/4/28
 */
@Slf4j
class MultiModalDialogUsage {
    static State.DialogState currentState;
    static MultiModalDialog conversation;
    static int enterListeningTimes = 0;
    static boolean vqaUseUrl = true;
    private final String workSpaceId = "";
    private final String appId = "";
    private final String modelName = "multimodal-dialog";

    void testMultimodalVQA() {
    /*
      step1. 发送”看看前面有什么东西“，onRespondingContent 返回visual_qa 指令
      step2. 发送图片列表
      step3. 返回图片的对话结果
      */
        System.out.println("############ Start Test VQA ############");
        vqaUseUrl = true;
        MultiModalRequestParam params =
                MultiModalRequestParam.builder()
                        .customInput(
                                MultiModalRequestParam.CustomInput.builder()
                                        .workspaceId(workSpaceId)
                                        .appId(appId)
                                        .build())
                        .upStream(
                                MultiModalRequestParam.UpStream.builder()
                                        .mode("push2talk")
                                        .audioFormat("pcm")
                                        .build())
                        .downStream(
                                MultiModalRequestParam.DownStream.builder()
                                        .voice("longxiaochun_v2")
                                        .sampleRate(48000)
                                        .build())
                        .clientInfo(
                                MultiModalRequestParam.ClientInfo.builder()
                                        .userId("1234")
                                        .device(MultiModalRequestParam.ClientInfo.Device.builder().uuid("device_1234").build())
                                        .build())
                        .model(modelName)
                        .apiKey("your-api-key")
                        .build();
        log.debug("params: {}", JsonUtils.toJson(params));
        conversation = new MultiModalDialog(params, getCallback());
        conversation.start();
        while (currentState != State.DialogState.LISTENING) {
            try {
                sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        // 模拟语音请求
        conversation.requestToRespond("prompt","拍照看看前面有什么东西",null);

        // 增加交互流程等待
        while (enterListeningTimes < 3) {
            try {
                sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        conversation.stop();
        try {
            sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("############ End Test VQA ############");
    }



    public static void main(String[] args) {
        MultiModalDialogUsage multiModalDialogUsage = new MultiModalDialogUsage();
        multiModalDialogUsage.testMultimodalVQA();
    }

    public static MultiModalDialogCallback getCallback() {
        return new MultiModalDialogCallbackImpl();
    }
    public static class MultiModalDialogCallbackImpl extends MultiModalDialogCallback {
        @Override
        public void onConnected() {}
        @Override
        public void onStarted(String dialogId) {
            log.info("onStarted: {}", dialogId);
        }
        @Override
        public void onStopped(String dialogId) {
            log.info("onStopped: {}", dialogId);
        }
        @Override
        public void onSpeechStarted(String dialogId) {
            log.info("onSpeechStarted: {}", dialogId);
        }
        @Override
        public void onSpeechEnded(String dialogId) {
            log.info("onSpeechEnded: {}", dialogId);
        }
        @Override
        public void onError(String dialogId, String errorCode, String errorMsg) {
            log.error("onError: {}, {}, {}", dialogId, errorCode, errorMsg);
            enterListeningTimes++ ; //force quit dialog test
        }
        @Override
        public void onStateChanged(State.DialogState state) {
            log.info("onStateChanged: {}", state);
            currentState = state;
            if (currentState == State.DialogState.LISTENING) {
                enterListeningTimes++;
                log.info("enterListeningTimes: {}", enterListeningTimes);
            }
        }
        @Override
        public void onSpeechAudioData(ByteBuffer audioData) {
            //write audio data to file
            //or redirect to audio player
        }
        @Override
        public void onRespondingStarted(String dialogId) {
            log.info("onRespondingStarted: {}", dialogId);
            conversation.localRespondingStarted();
        }

        @Override
        public void onRespondingEnded(String dialogId, JsonObject content) {
            log.info("onRespondingEnded: {}", dialogId);
            conversation.localRespondingEnded();
        }


        @Override
        public void onRespondingContent(String dialogId, JsonObject content) {
            log.info("onRespondingContent: {}, {}", dialogId, content);
            if (content.has("extra_info")) {
                JsonObject extraInfo = content.getAsJsonObject("extra_info");
                if (extraInfo.has("commands")) {
                    String commandsStr = extraInfo.get("commands").getAsString();
                    log.info("commandsStr: {}", commandsStr);
                    //"[{\"name\":\"visual_qa\",\"params\":[{\"name\":\"shot\",\"value\":\"拍照看看\",\"normValue\":\"True\"}]}]"
                    JsonArray commands = new Gson().fromJson(commandsStr, JsonArray.class);
                    for (JsonElement command : commands) {
                        JsonObject commandObj = command.getAsJsonObject();
                        if (commandObj.has("name")) {
                            String commandStr = commandObj.get("name").getAsString();
                            if (commandStr.equals("visual_qa")) {
                                log.info("拍照了！！！！");
                                MultiModalRequestParam.UpdateParams updateParams = MultiModalRequestParam.UpdateParams.builder()
                                        .images(getMockOSSImage())
                                        .build();
                                conversation.requestToRespond("prompt","",updateParams);
                            }
                        }
                    }
                }
            }
        }
        @Override
        public void onSpeechContent(String dialogId, JsonObject content) {
            log.info("onSpeechContent: {}, {}", dialogId, content);
        }
        @Override
        public void onRequestAccepted(String dialogId) {
            log.info("onRequestAccepted: {}", dialogId);
        }
        @Override
        public void onClosed() {
            log.info("onClosed");
            enterListeningTimes++ ;
        }
    }
    public static List<Object> getMockOSSImage() {
        JsonObject imageObject = new JsonObject();
        JsonObject extraObject = new JsonObject();
        List<Object> images = new ArrayList<>();
        try{
            if (vqaUseUrl){
                imageObject.addProperty("type", "url");
                imageObject.addProperty("value", "https://help-static-aliyun-doc.aliyuncs.com/assets/img/zh-CN/7043267371/p909896.png");
                imageObject.add("extra", extraObject);
            }else {
                imageObject.addProperty("type", "base64");
                imageObject.addProperty("value", getLocalImageBase64());
            }
            images.add(imageObject);
        }catch (Exception e){
            e.printStackTrace();
        }
        return images;
    }
    public static String getLocalImageBase64() {
        // 图片文件路径
        String imagePath = "./**/your-demo.jpg";
        try {
            FileInputStream fileInputStream = new FileInputStream(new File(imagePath));
            byte[] bytes = new byte[fileInputStream.available()];
            fileInputStream.read(bytes);
            fileInputStream.close();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}