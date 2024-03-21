// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.aigc.generation.models;

import com.alibaba.dashscope.aigc.generation.GenerationParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/** @deprecated use GenerationParam instead */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class QwenParam extends GenerationParam {}
