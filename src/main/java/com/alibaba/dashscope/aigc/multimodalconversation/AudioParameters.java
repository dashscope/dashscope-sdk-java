/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.dashscope.aigc.multimodalconversation;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Title audio parameters.<br>
 * Description audio parameters.<br>
 *
 * @author yuanci.ytb
 * @since 2.18.0
 */
@Data
@SuperBuilder
public class AudioParameters implements Serializable {
  /** Audio output voice, support: Cherry, Serena, Ethan and Chelsie. Default value: Cherry */
  @SerializedName("voice")
  private Voice voice;

  @Getter
  public enum Voice {
    @SerializedName("Cherry")
    CHERRY("Cherry"),

    @SerializedName("Serena")
    SERENA("Serena"),

    @SerializedName("Ethan")
    ETHAN("Ethan"),

    @SerializedName("Chelsie")
    CHELSIE("Chelsie"),

    @SerializedName("Dylan")
    DYLAN("Dylan"),

    @SerializedName("Jada")
    JADA("Jada"),

    @SerializedName("Sunny")
    SUNNY("Sunny"),

    @SerializedName("Nofish")
    NOFISH("Nofish"),

    @SerializedName("Jennifer")
    JENNIFER("Jennifer"),

    @SerializedName("Li")
    LI("Li"),

    @SerializedName("Marcus")
    MARCUS("Marcus"),

    @SerializedName("Roy")
    ROY("Roy"),

    @SerializedName("Peter")
    PETER("Peter"),

    @SerializedName("Eric")
    ERIC("Eric"),

    @SerializedName("Rocky")
    ROCKY("Rocky"),

    @SerializedName("Kiki")
    KIKI("Kiki"),

    @SerializedName("Ryan")
    RYAN("Ryan"),

    @SerializedName("Katerina")
    KATERINA("Katerina"),

    @SerializedName("Elias")
    ELIAS("Elias");

    private final String value;

    Voice(String value) {
      this.value = value;
    }
  }
}
