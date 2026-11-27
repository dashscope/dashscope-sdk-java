# DashScope Java SDK 3.0 Examples

English | [中文](README_zh.md)

The examples use a four-layer structure (design document §8): one file per capability, minimal
runnable programs, and class names in the form `<resource><verb>`. The API key is read only from
`DASHSCOPE_API_KEY` (or `-Ddashscope.api.key=`). If it is missing, the example prints setup
instructions and exits with a non-zero status.

```
01_atomic_capabilities/    # Atomic capabilities: one API per file
02_advanced_engineering/   # Engineering: retry/circuit breaking, caching, local mock debugging
03_agent_recipes/          # Agent recipes: function calling, RAG, AgentStudio
04_real_world_scenarios/   # Scenario harnesses: customer service, production RAG, meetings, ticket triage
legacy_2x/                 # 66 archived 2.x examples (not compiled); see legacy_2x/README.md
```

The active four-layer examples use the 3.0 API under the `com.alibaba.dashscope` root package.
The `legacy_2x/` directory retains the historical FQCNs. To reuse those examples with 3.0, change
2.x imports to `com.alibaba.dashscope.legacy_2x.*` and AgentStudio imports to
`com.alibaba.dashscope.applications.agentstudio.*`; see the archive README for details.
All public top-level types in the SDK's `legacy_2x` package carry type-level `@Deprecated`
annotations, so javac reports their use as deprecated. Package declarations retain only
`@deprecated` Javadoc tags for Java 8 compatibility. AgentStudio and `shared` are not deprecated.

## Setup

```bash
# From the repository root, install the SDK locally first (the examples resolve version 3.0.0)
JAVA_HOME=<jdk8+> mvn install -DskipTests -Dgpg.skip=true -Dmaven.javadoc.skip=true

export DASHSCOPE_API_KEY=sk-...
# Optional: DASHSCOPE_REGION / DASHSCOPE_WORKSPACE / DASHSCOPE_BASE_URL
```

Run examples from the repository root, using `RUN` as the command prefix for the tables below:

```bash
RUN="mvn -q -f examples/pom.xml compile exec:java -Dexec.mainClass="
$RUN com.alibaba.dashscope.examples.atomic.chat.ChatCompletionsCreate
```

You can also run `mvn -o -f examples/pom.xml compile` first and then invoke `java -cp` directly,
with `examples/target/classes` and the SDK jar on the classpath.

Examples that write files never overwrite existing files. Audio playback examples degrade
gracefully when no sound device is available (or when `--no-play` is supplied). Real API calls may
incur charges. Batch-job examples print the `task_id` before waiting, so an interrupted run can be
resumed with `--task-id` instead of submitting and paying for the same job again.

## Dry Runs (Local and CI)

```bash
mvn -f examples/pom.xml clean test
```

> `examples/` is a standalone Maven project and is not declared as a `<module>` in the root
> `pom.xml`. In `build-and-test.yml`, the test job runs `mvn install -DskipTests -Dgpg.skip=true`
> after the root `mvn clean test` and before the command above. This ensures that clean example
> compilation and MockWebServer dry runs use the SDK from the same checkout rather than a published
> version. Run the SDK installation command from [Setup](#setup) before local verification as well.
> Both the SDK and examples default to JDK 8 source/target 8 compilation. On JDK 9+, the
> `jdk9-plus` profile automatically enables release 8 API checking.
>
> Formatting checks remain separate: the `./*/src/.*java` filters in `lint.sh` and `linter.yml`
> match the three files under `examples/src/test/java/` (`ExamplesDryRunTest`,
> `DryRunDispatcher`, and `MockWs`), which must comply with google-java-format (100-column limit).
> The other 109 files (43 active four-layer files: 41 runnable examples plus the two shared support
> classes `ExampleSupport`/`AudioCommon`, and 66 archived `legacy_2x/` examples) are outside this
> formatting gate; the archived examples are not compiled.

`ExamplesDryRunTest` uses MockWebServer to emulate every HTTP/SSE/WebSocket endpoint and runs each
example's `main` method in a child JVM, with apiKey/baseUrl/wsBaseUrl pointing to the mock via `-D`.
It asserts a zero exit code. Two examples require a real environment and are excluded from dry
runs: `AsrMicrophoneRecognize` (microphone hardware) and `AsrJobsTranscribe` (requires an https://
result download URL). `TokenizationLocalCount` runs offline; its dry run injects a synthetic vocab
through `DASHSCOPE_QWEN_VOCAB` and does not download from ModelScope.

## 01 Atomic Capabilities: Text, Multimodal, and Embeddings

| Capability | Class (omit the `com.alibaba.dashscope.examples.atomic.` prefix) | API path | Arguments appended to `$RUN<class>` |
|---|---|---|---|
| Chat completions (sync + SSE) | `chat.ChatCompletionsCreate` | `client.chat().completions().create / createStreaming` | None |
| Chat completions (async Future/Flowable) | `chat.ChatCompletionsCreateAsync` | `asyncClient.chat().completions().create / createStreaming` | None |
| Chat completions (J7 encrypted channel) | `chat.ChatCompletionsCreateEncrypted` | `create(params, RequestOptions.enableEncryption(true))` | None |
| Token counting | `chat.TokenizationCount` | `client.tokenization().count` | None |
| Local tokenization (offline BPE) | `chat.TokenizationLocalCount` | `TokenizerFactory.qwen()` (no apiKey required) | None |
| Multimodal chat (image + text) | `multimodal.MultimodalChatCreate` | `client.multimodal().chat().create / createStreaming` | None |
| Text embeddings | `embeddings.EmbeddingsCreate` | `client.embeddings().create` | None |
| Reranking | `rerank.RerankCreate` | `client.rerank().create` | None |
| Text-to-image (job) | `images.ImagesGenerate` | `client.images().generate / jobs().create/wait/fetch/list` | None |
| Text-to-video (job) | `videos.VideosGenerate` | `client.videos().generate / jobs().*` | None |
| File management | `files.FilesManage` | `client.files().upload/list/get/delete` | None |
| Model listing | `models.ModelsList` | `client.models().list/get` | None |
| Fine-tuning jobs | `finetunes.FineTunesCreate` | `client.fineTunes().list/get` | None |
| Model deployments | `deployments.DeploymentsManage` | `client.deployments().list/get` | None |
| Application calls (App) | `apps.AppsCompletionsCreate` | `client.apps().completions().create / createStreaming` | None (`DASHSCOPE_APP_ID`) |
| Decision model | `decision.DecisionCreate` | `client.decision().create` | None |

## 01 Atomic Capabilities: Audio (omit the `atomic.audio.` prefix)

| Capability | Class | API path | Arguments |
|---|---|---|---|
| WebSocket duplex recognition | `AsrWebsocketRecognize` | `client.audio().asr().recognize` | `<speech.wav> [model]` (16 kHz, mono, 16-bit) |
| HTTP recorded-file recognition (job) | `AsrJobsTranscribe` | `jobs().create("/services/audio/asr/transcription") + jobs().wait` | `--url <https> …` or `--task-id <id>` |
| REST streaming recognition (SSE) | `AsrHttpStreamRecognize` | `client.multimodal().chat().createStreaming` | `--file <wav>` or `--url <https>` |
| Real-time microphone recognition | `AsrMicrophoneRecognize` | `client.audio().asr().recognize` | `[--seconds 5] [--recording x.wav]` |
| One-shot HTTP synthesis | `TtsHttpSynthesize` | `client.audio().tts().http().synthesize` | `<text> <out.wav> [--no-play]` |
| HTTP streaming synthesis | `TtsHttpStreamSynthesize` | `client.audio().tts().http().stream` | `<text> <out.wav> [--no-play]` |
| WebSocket streaming synthesis | `TtsWebsocketConnect` | `client.audio().tts().connect` (adds `chat().completions().createStreaming` with `--prompt`) | `--text t…` / `--text-file f` / `--prompt p` + `--output o.wav` |
| Voice clone management | `VoiceCloneManage` | `client.audio().voices().create/get/list/delete` | `create --url <https>` / `get` / `list` / `delete --yes` |
| TingWu offline job | `TingWuJobsCreate` | `client.tingwu().jobs().create/wait` | `--url <file url>` |
| Five tasks over one connection | `AudioConnectionReuse` | `client.wsConnection()` + `recognize`/`tts().connect(connection=)` | `asr <wav> --output-dir <new directory>` or `tts --output-dir <new directory>` |
| Single-turn Omni voice dialog | `RealtimeOmniDialog` | `client.audio().realtime().connect` (commit + createResponse) | `--wav in.wav --output o.wav [--no-play]` |
| Qwen-Audio voice dialog | `RealtimeQwenAudioDialog` | Same as above (flat audio format parameters) | Same as above |
| Real-time speech translation | `RealtimeTranslation` | Same as above (server VAD + endSession) | `--wav in.wav --language en --output o.wav` |

## 02 Engineering (omit the `engineering.` prefix)

| Capability | Class | Description |
|---|---|---|
| Backoff retry + cross-region circuit breaking | `resilience.CircuitBreakerRetry` | Includes two local HttpServers; no API key required |
| Client-side semantic cache | `cache.ClientSideCache` | Skips the chat call after an embedding cosine-similarity cache hit |
| Local mock debugging | `testing.LocalMockDebugging` | Includes an HttpServer and demonstrates error paths and `raiseOnError(false)` |

## 03 Agent Recipes (omit the `recipes.` prefix)

| Capability | Class | API path |
|---|---|---|
| Multi-turn function-calling loop | `chat.ToolCallLoop` | `client.chat().completions().create(tools)` |
| RAG expert system | `rag.RagExpertSystem` | Embeddings + rerank + chat pipeline |
| AgentStudio quick start | `agentstudio.AgentStudioQuickStart` | `client.agentStudio()`: environments/agents/sessions/events.stream(SSE)/archive |
| AgentStudio scheduled deployments | `agentstudio.AgentStudioDeploymentsManage` | `deployments().create/run/listRuns` (`DASHSCOPE_AGENT_ID`) |
| AgentStudio memory stores | `agentstudio.AgentStudioMemoryStores` | `memoryStores()`: store/memory lifecycle, versions, redact |

AgentStudio is a business package shared by both generations and resides under
`com.alibaba.dashscope.applications.agentstudio`. The `applications` package is only a grouping;
the entry point remains `client.agentStudio()`. The 3.0 `baseUrl` does not apply to AgentStudio.
Region and workspace follow the client configuration, and `DASHSCOPE_AGENTSTUDIO_URL` can specify
a custom endpoint directly.

## 04 Scenario Harnesses (omit the `scenarios.` prefix)

| Scenario | Class | Description |
|---|---|---|
| Customer-service voice workflow | `customerservice.CustomerServiceAssistant` | Multi-turn function-calling conversation |
| Production-grade RAG | `rag.ProductionRagHarness` | Offline by default (includes two regional HttpServers); set `RAG_HARNESS_LIVE=1` for real services |
| Real-time audio meeting | `meeting.RealtimeMeetingHarness` | Offline scripted replay by default (including reconnection and business-failure fallback); set `MEETING_HARNESS_LIVE=1` for real-time TingWu |
| Ticket triage | `triage.TicketTriageHarness` | Offline in-process decision backend by default; set `TRIAGE_HARNESS_LIVE=1` for the decision model |

## Path to Production

The examples remain minimal and runnable. For production guidance, see the circuit-breaking,
caching, and mock examples in layer 02 and the three harnesses in layer 04. The internal document
`../md/dashscope-3.0-migration.md` (not distributed with this repository) covers error-code
semantics; see `legacy_2x/README.md` for archived 2.x examples.
