# DashScope Java SDK 3.0 示例

四层结构（设计文档 §8）：每能力一文件、最小可运行；类名 `<资源><动词>`；apiKey 仅读
`DASHSCOPE_API_KEY`（或 `-Ddashscope.api.key=`），缺失时打印指引并非零退出。

```
01_atomic_capabilities/    # 原子能力：一个文件演示一个接口
02_advanced_engineering/   # 工程化：重试熔断、缓存、本地 mock 调试
03_agent_recipes/          # Agent 配方：function calling、RAG、AgentStudio
04_real_world_scenarios/   # 场景 harness：客服、生产 RAG、会议纪要、工单分诊
legacy_2x/                 # 2.x 旧样例 64 个，整体封存（不编译），见 legacy_2x/README.md
```

活动四层示例使用 `com.alibaba.dashscope` 根包 3.0 API。`legacy_2x/` 保留历史旧 FQCN；
复用其中样例时，2.x imports 须调整为 `com.alibaba.dashscope.legacy_2x.*`，AgentStudio
imports 须调整为 `com.alibaba.dashscope.applications.agentstudio.*`，详见归档目录说明。
SDK 的 `legacy_2x` 全部公开顶层类型带类型级 `@Deprecated`，使用时 javac 会报告弃用；
包级仅保留 `@deprecated` Javadoc 标签以兼容 Java 8。AgentStudio 与 `shared` 不废弃。

## 准备

```bash
# 仓库根目录：先把 SDK 装进本地仓库（示例模块按 3.0.0 解析）
JAVA_HOME=<jdk8+> mvn install -DskipTests -Dgpg.skip=true -Dmaven.javadoc.skip=true

export DASHSCOPE_API_KEY=sk-...
# 可选：DASHSCOPE_REGION / DASHSCOPE_WORKSPACE / DASHSCOPE_BASE_URL
```

运行方式（仓库根目录执行，`RUN` 作为下表命令前缀）：

```bash
RUN="mvn -q -f examples/pom.xml compile exec:java -Dexec.mainClass="
$RUN com.alibaba.dashscope.examples.atomic.chat.ChatCompletionsCreate
```

也可先 `mvn -o -f examples/pom.xml compile`，再用 `java -cp` 直接跑（classpath 含
`examples/target/classes` 与 SDK jar）。

约定：写文件类示例一律不覆盖已有文件；语音播放示例在无声卡环境自动降级（或加
`--no-play`）；真实调用可能计费，批任务类示例先打印 `task_id` 再等待，中断后可用
`--task-id` 恢复而不是重复付费提交。

## 干跑（本地与 CI）

```bash
mvn -f examples/pom.xml clean test
```

> `examples/` 是独立 Maven 工程（根 `pom.xml` 未声明 `<module>`）。`build-and-test.yml`
> 的 test job 在根 `mvn clean test` 后先执行 `mvn install -DskipTests -Dgpg.skip=true`，再执行上述命令，
> 确保示例 clean 编译与 MockWebServer 干跑依赖同一 checkout 的当前 SDK，而非已发布版本。
> 本地验证也应先执行「准备」中的 SDK 安装命令。SDK 与示例都默认以 JDK 8 的 source/target 8
> 编译，JDK 9+ 自动启用 `jdk9-plus` profile 的 release 8 API 校验。
>
> 格式门禁独立保留：`lint.sh` 与 `linter.yml` 的过滤器 `./*/src/.*java` 会命中
> `examples/src/test/java/` 下的 3 个文件（`ExamplesDryRunTest`、`DryRunDispatcher`、`MockWs`），
> 它们必须符合 google-java-format（100 列上限）；其余 102 个文件（38 个四层活动示例 + 64 个
> 封存的 `legacy_2x/`，后者不参与编译）不在该门禁范围内。

`ExamplesDryRunTest` 用 MockWebServer 扮演全部 HTTP/SSE/WebSocket 端点，把每个示例的
`main` 在子 JVM 里真实跑通（apiKey/baseUrl/wsBaseUrl 经 `-D` 指向 mock），断言退出码为 0。
两个例外需要真实环境，不在干跑内：`AsrMicrophoneRecognize`（麦克风硬件）、
`AsrJobsTranscribe`（强制 https:// 结果下载地址）。

## 01 原子能力 · 文本 / 多模态 / 向量

| 能力 | 类（`com.alibaba.dashscope.examples.atomic.` 前缀从略） | API 路径 | 运行命令（`$RUN<类>` 后追加） |
|---|---|---|---|
| 对话补全（同步+SSE） | `chat.ChatCompletionsCreate` | `client.chat().completions().create / createStreaming` | 无参数 |
| 分词计数 | `chat.TokenizationCount` | `client.tokenization().count` | 无参数 |
| 多模态对话（图文） | `multimodal.MultimodalChatCreate` | `client.multimodal().chat().create / createStreaming` | 无参数 |
| 文本向量 | `embeddings.EmbeddingsCreate` | `client.embeddings().create` | 无参数 |
| 重排序 | `rerank.RerankCreate` | `client.rerank().create` | 无参数 |
| 文生图（Job） | `images.ImagesGenerate` | `client.images().generate / jobs().create/wait/fetch/list` | 无参数 |
| 文生视频（Job） | `videos.VideosGenerate` | `client.videos().generate / jobs().*` | 无参数 |
| 文件管理 | `files.FilesManage` | `client.files().upload/list/get/delete` | 无参数 |
| 模型列表 | `models.ModelsList` | `client.models().list/get` | 无参数 |
| 微调任务 | `finetunes.FineTunesCreate` | `client.fineTunes().list/get` | 无参数 |
| 应用调用（App） | `apps.AppsCompletionsCreate` | `client.apps().completions().create / createStreaming` | 无参数（`DASHSCOPE_APP_ID`） |
| 决策模型 | `decision.DecisionCreate` | `client.decision().create` | 无参数 |

## 01 原子能力 · 语音（`atomic.audio.` 前缀从略）

| 能力 | 类 | API 路径 | 运行参数 |
|---|---|---|---|
| WebSocket 双工识别 | `AsrWebsocketRecognize` | `client.audio().asr().recognize` | `<speech.wav> [model]`（16 kHz 单声道 16-bit） |
| HTTP 录音文件识别（Job） | `AsrJobsTranscribe` | `jobs().create("/services/audio/asr/transcription") + jobs().wait` | `--url <https> …` 或 `--task-id <id>` |
| REST 流式识别（SSE） | `AsrHttpStreamRecognize` | `client.multimodal().chat().createStreaming` | `--file <wav>` 或 `--url <https>` |
| 麦克风实时识别 | `AsrMicrophoneRecognize` | `client.audio().asr().recognize` | `[--seconds 5] [--recording x.wav]` |
| HTTP 一次性合成 | `TtsHttpSynthesize` | `client.audio().tts().http().synthesize` | `<text> <out.wav> [--no-play]` |
| HTTP 流式合成 | `TtsHttpStreamSynthesize` | `client.audio().tts().http().stream` | `<text> <out.wav> [--no-play]` |
| WebSocket 流式合成 | `TtsWebsocketConnect` | `client.audio().tts().connect`（`--prompt` 时叠加 `chat().completions().createStreaming`） | `--text t…` / `--text-file f` / `--prompt p` + `--output o.wav` |
| 声音复刻管理 | `VoiceCloneManage` | `client.audio().voices().create/get/list/delete` | `create --url <https>` / `get` / `list` / `delete --yes` |
| 听悟离线任务 | `TingWuJobsCreate` | `client.tingwu().jobs().create/wait` | `--url <file url>` |
| 单连接五任务复用 | `AudioConnectionReuse` | `client.wsConnection()` + `recognize`/`tts().connect(connection=)` | `asr <wav> --output-dir <新目录>` 或 `tts --output-dir <新目录>` |
| Omni 单轮语音对话 | `RealtimeOmniDialog` | `client.audio().realtime().connect`（commit + createResponse） | `--wav in.wav --output o.wav [--no-play]` |
| Qwen-Audio 语音对话 | `RealtimeQwenAudioDialog` | 同上（flat 音频格式参数） | 同上 |
| 实时语音翻译 | `RealtimeTranslation` | 同上（server VAD + endSession） | `--wav in.wav --language en --output o.wav` |

## 02 工程化（`engineering.` 前缀从略）

| 能力 | 类 | 说明 |
|---|---|---|
| 退避重试 + 熔断跨地域容灾 | `resilience.CircuitBreakerRetry` | 自带双本地 HttpServer，无需密钥即可运行 |
| 客户端语义缓存 | `cache.ClientSideCache` | embedding 余弦相似度命中后跳过 chat 调用 |
| 本地 mock 调试 | `testing.LocalMockDebugging` | 自带 HttpServer，演示错误分支与 `raiseOnError(false)` |

## 03 Agent 配方（`recipes.` 前缀从略）

| 能力 | 类 | API 路径 |
|---|---|---|
| Function calling 多轮循环 | `chat.ToolCallLoop` | `client.chat().completions().create(tools)` |
| RAG 专家系统 | `rag.RagExpertSystem` | embeddings + rerank + chat 串联 |
| AgentStudio 快速开始 | `agentstudio.AgentStudioQuickStart` | `client.agentStudio()`：environments/agents/sessions/events.stream(SSE)/archive |
| AgentStudio 定时部署 | `agentstudio.AgentStudioDeploymentsManage` | `deployments().create/run/listRuns`（`DASHSCOPE_AGENT_ID`） |

AgentStudio 为两代共用业务包，位于 `com.alibaba.dashscope.applications.agentstudio`；
`applications` 仅作包分组，入口仍为 `client.agentStudio()`。3.0 的 `baseUrl` 不作用于它，
地域/工作空间随 client 配置，也可用 `DASHSCOPE_AGENTSTUDIO_URL` 直连自定义地址。

## 04 场景 harness（`scenarios.` 前缀从略）

| 场景 | 类 | 说明 |
|---|---|---|
| 客服语音链路 | `customerservice.CustomerServiceAssistant` | 多轮 function calling 对话 |
| 生产级 RAG | `rag.ProductionRagHarness` | 默认离线（自带双地域 HttpServer）；`RAG_HARNESS_LIVE=1` 走真实服务 |
| 实时音频会议 | `meeting.RealtimeMeetingHarness` | 默认离线脚本化回放（含断连重连/业务失败降级）；`MEETING_HARNESS_LIVE=1` 走听悟实时 |
| 工单分诊 | `triage.TicketTriageHarness` | 默认离线 in-process 决策后端；`TRIAGE_HARNESS_LIVE=1` 走决策模型 |

## 生产化路径

示例保持最小可运行；生产化请参考 02 层的熔断/缓存/mock 三件套与 04 层的三个 harness，
错误码心智见 `../md/dashscope-3.0-migration.md`，2.x 旧样例见 `legacy_2x/README.md`。
