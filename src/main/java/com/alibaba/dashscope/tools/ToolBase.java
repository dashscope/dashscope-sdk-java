package com.alibaba.dashscope.tools;

import com.alibaba.dashscope.common.TypeRegistry;
import com.alibaba.dashscope.tools.T2Image.Text2Image;
import com.alibaba.dashscope.tools.codeinterpretertool.ToolCodeInterpreter;
import com.alibaba.dashscope.tools.search.ToolQuarkSearch;
import com.alibaba.dashscope.tools.wanx.ToolWanX;
import lombok.experimental.SuperBuilder;

/** tool request base */
@SuperBuilder
public abstract class ToolBase implements ToolInterface {
  private static final TypeRegistry<ToolBase> toolRegistry = new TypeRegistry<>();

  protected static synchronized void registerTool(
      String toolType, Class<? extends ToolBase> clazz) {
    toolRegistry.register(toolType, clazz);
  }

  public static synchronized Class<? extends ToolBase> getToolClass(String toolType) {
    return toolRegistry.get(toolType);
  }

  // register official tools for list.
  static {
    registerTool("function", ToolFunction.class);
    registerTool("quark_search", ToolQuarkSearch.class);
    registerTool("code_interpreter", ToolCodeInterpreter.class);
    registerTool("wanx", ToolWanX.class);
    registerTool("text_to_image", Text2Image.class);
  }

  @Override
  public abstract String getType();
}
