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

    @SerializedName("Momo")
    MOMO("Momo"),

    @SerializedName("Rhodes")
    RHODES("Rhodes"),

    @SerializedName("Vivian")
    VIVIAN("Vivian"),

    @SerializedName("Moon")
    MOON("Moon"),

    @SerializedName("Maia")
    MAIA("Maia"),

    @SerializedName("Kai")
    KAI("Kai"),

    @SerializedName("Nofish")
    NOFISH("Nofish"),

    @SerializedName("Bella")
    BELLA("Bella"),

    @SerializedName("Jennifer")
    JENNIFER("Jennifer"),

    @SerializedName("Aiden")
    AIDEN("Aiden"),

    @SerializedName("Bodega")
    BODEGA("Bodega"),

    @SerializedName("Sonrisa")
    SONRISA("Sonrisa"),

    @SerializedName("Alek")
    ALEK("Alek"),

    @SerializedName("Dolce")
    DOLCE("Dolce"),

    @SerializedName("Ivan")
    IVAN("Ivan"),

    @SerializedName("Sohee")
    SOHEE("Sohee"),

    @SerializedName("Ono Anna")
    ONO_ANNA("Ono Anna"),

    @SerializedName("Lenn")
    LENN("Lenn"),

    @SerializedName("Inna")
    INNA("Inna"),

    @SerializedName("Najm")
    NAJM("Najm"),

    @SerializedName("Lamia")
    LAMIA("Lamia"),

    @SerializedName("Emilien")
    EMILIEN("Emilien"),

    @SerializedName("Andre")
    ANDRE("Andre"),

    @SerializedName("Radio Gol")
    RADIO_GOL("Radio Gol"),

    @SerializedName("Li")
    LI("Li"),

    @SerializedName("Marcus")
    MARCUS("Marcus"),

    @SerializedName("Roy")
    ROY("Roy"),

    @SerializedName("Peter")
    PETER("Peter"),

    @SerializedName("Christy")
    CHRISTY("Christy"),

    @SerializedName("Rocky")
    ROCKY("Rocky"),

    @SerializedName("Kiki")
    KIKI("Kiki"),

    @SerializedName("Eric")
    ERIC("Eric"),

    @SerializedName("Eldric Sage")
    ELDRIC_SAGE("Eldric Sage"),

    @SerializedName("Mia")
    MIA("Mia"),

    @SerializedName("Mochi")
    MOCHI("Mochi"),

    @SerializedName("Bellona")
    BELLONA("Bellona"),

    @SerializedName("Vincent")
    VINCENT("Vincent"),

    @SerializedName("Bunny")
    BUNNY("Bunny"),

    @SerializedName("Neil")
    NEIL("Neil"),

    @SerializedName("Elias")
    ELIAS("Elias"),

    @SerializedName("Arthur")
    ARTHUR("Arthur"),

    @SerializedName("Nini")
    NINI("Nini"),

    @SerializedName("Ebona")
    EBONA("Ebona"),

    @SerializedName("Seren")
    SEREN("Seren"),

    @SerializedName("Pip")
    PIP("Pip"),

    @SerializedName("Stella")
    STELLA("Stella");

    private final String value;

    Voice(String value) {
      this.value = value;
    }
  }
}
