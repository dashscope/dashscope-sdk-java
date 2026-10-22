// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.agentstudio.support.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 3.0 copy of the 2.x marker: fields excluded from Gson serialization. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface GsonExclude {}
