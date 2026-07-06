package com.alibaba.dashscope.audio.asr.translation.results;

import com.alibaba.dashscope.audio.asr.translation.TranslationRecognizerUsage;
import java.util.ArrayList;
import lombok.Getter;
import lombok.Setter;

public class TranslationRecognizerResultPack {
  @Setter @Getter private String requestId = "";
  @Getter private ArrayList<TranslationRecognizerUsage> usageList = new ArrayList<>();
  @Getter private ArrayList<TranscriptionResult> transcriptionResultList = new ArrayList<>();
  @Getter private ArrayList<TranslationResult> translationResultList = new ArrayList<>();
  @Setter @Getter private Throwable error = null;
}
