// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.atomic.models;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.v3.types.models.Model;
import com.alibaba.dashscope.v3.types.models.ModelList;

/**
 * Model catalog: paged list, provider-filtered list, and get-by-name. Translation of the Python
 * blueprint example 01_atomic_capabilities/models/models.py.
 */
public final class ModelsList {

  public static void main(String[] args) throws Exception {
    String model = System.getenv().getOrDefault("DASHSCOPE_MODEL", "qwen3.8-max");

    try (DashScopeClient client = ExampleSupport.client()) {
      // List models (paged)
      System.out.println("----- list models -----");
      ModelList page = client.models().list(1, 10);
      System.out.println("total: " + page.getTotalCount() + ", page " + page.getPageNo());
      for (Model item : page.getModels()) {
        System.out.println("model: " + item.getModel());
      }

      // List only Model Studio (Bailian) models
      System.out.println("----- list models (provider=Bailian) -----");
      page = client.models().list(1, 10, "Bailian", RequestOptions.NONE);
      page.getModels().stream()
          .limit(5)
          .forEach(item -> System.out.println("model: " + item.getModel()));

      // Get one model by name
      System.out.println("----- get model -----");
      Model item = client.models().get(model);
      System.out.println("model: " + item.getModel());
    }
  }
}
