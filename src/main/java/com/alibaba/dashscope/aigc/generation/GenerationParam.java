// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.aigc.generation;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class GenerationParam extends GenerationParamBase {}
