package com.alibaba.dashscope.threads.messages;

import com.alibaba.dashscope.base.FlattenHalfDuplexParamBase;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/** base message params */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public abstract class MessageParamBase extends FlattenHalfDuplexParamBase {}
