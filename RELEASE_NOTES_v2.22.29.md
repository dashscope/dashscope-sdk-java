# DashScope Java SDK v2.22.29 Release Notes

## 发布日期
2026-08-07

## 版本信息
- **版本号**: 2.22.29
- **Maven 坐标**: `com.alibaba:dashscope-sdk-java:2.22.29`

## 主要变更

### 新增功能
- **Managed Agent Deployment APIs** (#253)
  - 添加 managed agent deployment 相关 API
  - 支持 agent 的部署和管理功能

### 废弃标记
- **API 废弃标记** (#254)
  - 标记部分旧版 API 为 deprecated
  - 建议用户迁移到新版 API

## 安装方式

### Maven
```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>dashscope-sdk-java</artifactId>
    <version>2.22.29</version>
</dependency>
```

### Gradle
```gradle
implementation 'com.alibaba:dashscope-sdk-java:2.22.29'
```

## 兼容性说明
- 保持与 v2.22.28 的向后兼容性
- 部分 API 已标记为 deprecated，将在未来版本中移除
- 建议尽快迁移到新版 API

## 变更统计
- 提交数: 2
- 贡献者: kevin Lu, coolsky99
- 修复问题: 0
- 新增功能: 1

## 相关链接
- [GitHub Repository](https://github.com/dashscope/dashscope-sdk-java)
- [Maven Central](https://central.sonatype.com/artifact/com.alibaba/dashscope-sdk-java/2.22.29)
- [Issue Tracker](https://github.com/dashscope/dashscope-sdk-java/issues)

## 升级指南
从 v2.22.28 升级到 v2.22.29：
1. 更新 pom.xml 或 build.gradle 中的版本号
2. 检查是否有使用 deprecated API 的代码，计划迁移到新版 API
3. 运行测试确保功能正常

## 已知问题
暂无

## 感谢贡献者
感谢以下贡献者对本版本的贡献：
- kevin Lu
- coolsky99
