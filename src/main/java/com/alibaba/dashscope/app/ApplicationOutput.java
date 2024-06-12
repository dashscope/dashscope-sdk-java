// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.app;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Title Application call output parameters.<br>
 * Description Application call output parameters.<br>
 * Created at 2024-02-23 17:30
 *
 * @since jdk8
 */
@Data
@SuperBuilder
@ToString
public class ApplicationOutput {
  /** Completion call response text */
  @SerializedName("text")
  private String text;

  /** Finish reason of model generation stop */
  @SerializedName("finish_reason")
  private String finishReason;

  /** Session id for multiple round calls */
  @SerializedName("session_id")
  private String sessionId;

  /** Thoughts of model planning for app */
  @SerializedName("thoughts")
  private List<Thought> thoughts;

  /** Doc references for retrieval result */
  @SerializedName("doc_references")
  private List<DocReference> docReferences;

  @SuperBuilder
  @Data
  @ToString
  public static class Thought {
    /** Model's inference thought for rag or plugin process */
    @SerializedName("thought")
    private String thought;

    /** action type response : final response api: to run api calls */
    @SerializedName("action_type")
    private String actionType;

    /** model's results */
    @SerializedName("response")
    private String response;

    /** action name, e.g. searchDocument、api */
    @SerializedName("action_name")
    private String actionName;

    /** code of action, means which plugin or action to be run */
    @SerializedName("action")
    private String action;

    /** input param with stream */
    @SerializedName("action_input_stream")
    private String actionInputStream;

    /** api or plugin input parameters */
    @SerializedName("action_input")
    private JsonObject actionInput;

    /** result of api call or doc retrieval */
    @SerializedName("observation")
    private String observation;
  }

  @SuperBuilder
  @Data
  @ToString
  public static class DocReference {
    /** index id of doc retrival result reference */
    @SerializedName("index_id")
    private String indexId;

    /** title of original doc that retrieved */
    @SerializedName("title")
    private String title;

    /** id of original doc that retrieved */
    @SerializedName("doc_id")
    private String docId;

    /** name of original doc that retrieved */
    @SerializedName("doc_name")
    private String docName;

    /** url of original doc that retrieved */
    @SerializedName("doc_url")
    private String docUrl;

    /** text in original doc that retrieved */
    @SerializedName("text")
    private String text;

    /** Biz id that caller is able to associated for biz logic */
    @SerializedName("biz_id")
    private String bizId;

    /** List of referenced image URLs */
    @SerializedName("images")
    private List<String> images;
  }
}
