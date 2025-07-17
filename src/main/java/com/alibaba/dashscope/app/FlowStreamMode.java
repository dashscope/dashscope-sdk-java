// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.app;

public enum FlowStreamMode {

    /**
     * The streaming results from all nodes will be output in the thoughts field.
     */
    FULL_THOUGHTS("full_thoughts"),

    /**
     * Use the same output pattern as the agent application.
     */
    AGENT_FORMAT("agent_format"),

    /**
     * Use the output node and end node to perform the output.
     */
    MESSAGE_FORMAT("message_format");

    private final String value;

    private FlowStreamMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
