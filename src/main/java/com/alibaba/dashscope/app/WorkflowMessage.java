package com.alibaba.dashscope.app;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class WorkflowMessage {

    @SerializedName("node_id")
    private String nodeId;

    @SerializedName("node_name")
    private String nodeName;

    @SerializedName("node_type")
    private String nodeType;

    @SerializedName("node_status")
    private String nodeStatus;

    @SerializedName("node_is_completed")
    private Boolean nodeIsCompleted;

    @SerializedName("node_msg_seq_id")
    private Integer nodeMsgSeqId;

    @SerializedName("message")
    private Message message;

    @Data
    public static class Message {
        @SerializedName("role")
        private String role;

        @SerializedName("content")
        private String content;
    }
}
