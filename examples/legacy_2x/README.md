# legacy_2x —— 2.x 样例封存

本目录是 2.x 时代的 64 个平铺样例，自 `samples/` 整体迁入封存（git 历史保留）。

- **冻结**：不再新增、不再演进；文件面向 2.x API（`Generation`、`MultiModalConversation`、`com.alibaba.dashscope.agentstudio.*` 等），这些 API 在 3.0 同 artifact 中原样保留，样例语义仍然有效。
- **不参与构建**：本目录不在 examples 模块的编译源根内。
- 新代码请参考上层四层 3.0 样例（`01_atomic_capabilities/` 起），迁移对照见 `md/dashscope-3.0-migration.md`。
