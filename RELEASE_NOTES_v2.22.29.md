## What's Changed

### New Features
- Add managed agent deployment APIs for AgentStudio (#253)
  - Implement Deployment, DeploymentRun, DeploymentSchedule models
  - Add Deployments and DeploymentRuns resource classes
  - Support create, list, get, update, delete operations
  - Add sample code: AgentStudioDeployments.java

### Bug Fixes
- Serialize empty deployment metadata correctly
- Parse nested error envelopes in HTTP responses
- Restrict deployment metadata to string values only

### Deprecations
- Mark Assistants API as deprecated (#254)
  - Add @Deprecated annotations to Assistant, AssistantFile, Assistants classes
  - Add deprecation warnings to Runs class
  - Users should migrate to new API alternatives

### Code Quality
- Add comprehensive test coverage for AgentStudio Deployments
- Add test resources: agentstudio.json
- Improve code documentation and type safety

## Full Changelog
https://github.com/dashscope/dashscope-sdk-java/compare/v2.22.28...v2.22.29

## 👥 Contributors
- coolsky99
- luk384090-cloud
