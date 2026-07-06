package com.alibaba.dashscope.aigc.multimodalconversation;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Title voice output.<br>
 * Description voice output.<br>
 *
 * @author yuanci.ytb
 * @since 2.19.0
 */
@Data
@SuperBuilder
@NoArgsConstructor
public class AudioResult implements Serializable {
  @SerializedName("data")
  private String data;

  @SerializedName("id")
  private String id;

  @SerializedName("url")
  private String url;

  @SerializedName("expires_at")
  private Long expiresAt;
}
