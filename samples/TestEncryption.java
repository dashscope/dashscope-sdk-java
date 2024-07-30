import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.utils.EncryptionConfig;
import com.alibaba.dashscope.utils.EncryptionKey;
import com.alibaba.dashscope.utils.EncryptionKeys;
import com.alibaba.dashscope.utils.EncryptionUtils;
import com.alibaba.dashscope.utils.JsonUtils;
import io.reactivex.Flowable;

public class TestEncryption {
    public static void qwenQuickStartStream(EncryptionConfig encryptionConfig)
            throws NoApiKeyException, ApiException, InputRequiredException {
        Generation gen = new Generation(Protocol.HTTP.getValue());
        Message userMsg =
                Message.builder().role(Role.USER.getValue()).content("用萝卜、土豆、茄子做饭，给我个菜谱").build();
        GenerationParam param = GenerationParam.builder().model("qwen-plus").resultFormat("message")
                .messages(Arrays.asList(userMsg)).encryptionConfig(encryptionConfig).build();
        Flowable<GenerationResult> result = gen.streamCall(param);
        result.blockingForEach(msg -> {
            System.out.println(msg);
        });
        System.out.println(JsonUtils.toJson(result));
    }

    public static void qwenQuickStartGeneral(EncryptionConfig encryptionConfig)
            throws NoApiKeyException, ApiException, InputRequiredException {
        Generation gen = new Generation(Protocol.HTTP.getValue());
        Message userMsg = Message.builder().role(Role.USER.getValue())
                .content("Tell me something about AI.").build();
        GenerationParam param = GenerationParam.builder().model("qwen-turbo").encryptionConfig(encryptionConfig).resultFormat("message")
                .messages(Arrays.asList(userMsg)).build();
        GenerationResult result = gen.call(param);
        System.out.println(JsonUtils.toJson(result));
    }

    /**
     * Get encrypt key from server.
     * @return
     * @throws ApiException
     * @throws NoApiKeyException
     */
    public static EncryptionKey getEncryptKey() throws ApiException, NoApiKeyException {
        EncryptionKeys encryptionKeys = new EncryptionKeys();
        return encryptionKeys.get();
    }

    public static void main(String[] args) throws ApiException, NoApiKeyException,
            InputRequiredException, NoSuchAlgorithmException {
        // Getting encrypt key via: getEncryptKey and paste key id and public key here.
        String publicKeyId = "";
        String publicKey = "";
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        EncryptionConfig encryptionConfig =
                EncryptionConfig.builder().base64PublicKey(publicKey).publicKeyId(publicKeyId)
                        .AESEncryptKey(EncryptionUtils.generateAESKey()).iv(iv).build();
        qwenQuickStartGeneral(encryptionConfig);
        qwenQuickStartStream(encryptionConfig);
        System.exit(0);

    }
}
