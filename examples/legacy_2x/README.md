# legacy_2x —— 2.x 样例封存

本目录是 2.x 时代的 64 个平铺样例，自 `samples/` 整体迁入封存（git 历史保留）。

- **冻结**：不再新增、不再演进；文件保留 2.x 时代的源码和 FQCN（`Generation`、`MultiModalConversation`、`com.alibaba.dashscope.agentstudio.*` 等），作为历史样例封存。
- **目录迁移**：3.0 同 artifact 中，除 AgentStudio 外的 2.x 实现已迁入 `com.alibaba.dashscope.legacy_2x.*`；AgentStudio 已迁至 `com.alibaba.dashscope.applications.agentstudio.*`。在本目录复用这些样例时须同步调整 imports，AgentStudio 异常处理遵循 3.0 errors。
- **不参与构建**：本目录不在 examples 模块的编译源根内，不保证复制后无需修改即可编译。
- 新代码请参考上层四层 3.0 样例（`01_atomic_capabilities/` 起），迁移对照见 `../../md/dashscope-3.0-migration.md`。
