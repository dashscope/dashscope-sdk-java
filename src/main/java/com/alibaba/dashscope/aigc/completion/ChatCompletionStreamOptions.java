// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.aigc.completion;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * @deprecated Use {@link com.alibaba.dashscope.common.StreamOptions} instead for a shared
 *     implementation across all service classes.
 */
@Deprecated
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ChatCompletionStreamOptions extends com.alibaba.dashscope.common.StreamOptions {}
