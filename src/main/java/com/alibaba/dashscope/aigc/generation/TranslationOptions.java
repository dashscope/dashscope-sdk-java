package com.alibaba.dashscope.aigc.generation;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
@Data
public class TranslationOptions {
    /** 源语言的英文全称 */
    @SerializedName("source_lang")
    private String sourceLang;

    /** 源语言的英文全称 */
    @SerializedName("target_lang")
    private String targetLang;

    /** 在使用领域提示功能时需要设置的领域提示语句 */
    private String domains;

    /** 在使用术语干预翻译功能时需要设置的术语数组 */
    private List<Term> terms;

    /** 在使用翻译记忆功能时需要设置的翻译记忆数组 */
    @SerializedName("tm_list")
    private List<Tm> tmList;

    @SuperBuilder
    @Data
    public static class Tm {
        String source;
        String target;
    }

    @SuperBuilder
    @Data
    public static class Term {
        String source;
        String target;
    }
}