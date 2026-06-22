# HANDOFF - TicketRush

## Latest Snapshot - 2026-06-22 Backend Showcase Package

Current goal:
- Stop treating TicketRush as a UI/page demo and present it as a backend engineering showcase.

Current stage:
- Implemented and verified.
- The current direction is: README first screen = engineering evidence, not product UI.
- The local page remains only as an auxiliary entry.

Completed in this slice:
- Added `scripts/demo-smoke.ps1`:
  - checks actuator health;
  - preloads stock `1000`;
  - submits rush request A;
  - submits request B with a new `requestId` and the same `idempotentKey`;
  - prints `StockFlow`, `DuplicateCode=A0429`, and `VirtualThread=True`.
- Added README media assets:
  - `docs/media/ticketrush-core-demo.gif`;
  - `docs/media/ticketrush-core-demo.png`.
- Reworked README top into `后端工程展示`:
  - core GIF;
  - smoke script;
  - expected evidence;
  - benchmark/governance evidence table.
- Updated `docs/demo-runbook.md` and `docs/github-showcase.md` so the main demonstration path starts from the smoke script and architecture evidence, not the UI page.
- Updated `SPEC.md` phase 7 checklist for the backend showcase package.

Main demo script now:
- Show README core GIF.
- Run `.\scripts\demo-smoke.ps1`.
- Explain `1000 -> 999 -> 999`, `requestId A -> B`, unchanged `idempotentKey`, `A0429`, `processedByVirtualThread=true`.
- Then show architecture and benchmark reports.

Verified:
- `powershell -ExecutionPolicy Bypass -File .\scripts\demo-smoke.ps1`: passed.
  - `StockFlow = 1000 -> 999 -> 999`;
  - `DuplicateCode = A0429`;
  - `VirtualThread = True`.
- With JDK 21: `mvn "-Dtest=StaticDemoConsoleHtmlTest" test`: 5 tests passed.
- Markdown link scan for README/runbook/showcase: passed.
- Media dimensions:
  - `docs/media/ticketrush-core-demo.gif`: `960x540`, 8 frames;
  - `docs/media/ticketrush-core-demo.png`: `960x540`.
- `git diff --check`: passed with README CRLF/LF warning only.

Next step only:
- Commit this backend showcase package, then stop unless the user asks for another TicketRush scope.

## Latest Snapshot - 2026-06-22 One-Click Demo Story

Current goal:
- Turn the local TicketRush page into a focused one-click rush-ticket story instead of a manual API/control console.

Current stage:
- Implemented and verified.
- No backend API, schema, proxy, payment, auth, or production config changes were made.
- The page still only calls existing APIs.

Completed:
- Reworked the first screen around `开始抢票演示`.
- Main visible flow:
  - reset Redis hot inventory to `1000`;
  - submit one rush request and show inventory `1000 -> 999`;
  - submit again with a new `requestId` and the same `idempotentKey`;
  - final evidence shows `1000 -> 999 -> 999`, requestId `A -> B`, idempotency passed, and async order not duplicated.
- Kept manual params and inventory strategy switching under `手动参数和高级策略`.
- Kept health, Elasticsearch search, executor benchmark, Prometheus/Grafana links in the secondary `高级验证` area.
- Removed the old main-flow dependency on separate visible buttons for initialize/rush/repeat.
- Updated `StaticDemoConsoleHtmlTest`, `README.md`, `docs/demo-runbook.md`, and `docs/github-showcase.md`.
- Regenerated all desktop README screenshots under `docs/screenshots/desktop/*.png`.

Verified:
- Inline JavaScript syntax check: passed.
- With JDK 21: `mvn "-Dtest=StaticDemoConsoleHtmlTest" test`: 5 tests passed.
- With JDK 21: `mvn package -DskipTests`: passed.
- Docker app redeploy: `docker compose up -d --no-deps --force-recreate app`; health became `UP`.
- Browser/CDP desktop walkthrough at `1440x900`:
  - visible heading `一场抢票请求怎么被系统接住`;
  - one-click demo completed with `stockStory = 1000 -> 999 -> 999`;
  - idempotency showed `通过`;
  - async order showed `未重复触发`;
  - no horizontal overflow.
- Screenshot dimension check: all `docs/screenshots/desktop/*.png` are `1440x900`.
- `git diff --check`: passed with README CRLF/LF warning only.

Next step only:
- Commit this one-click demo page slice, then stop unless the user asks for another TicketRush scope.

## Latest Snapshot - 2026-06-22 Demo Page Redesign Complete

Current goal:
- Make the TicketRush local demo page feel like a focused抢票链路演示台, not a broad API console or slogan-heavy UI.

Current stage:
- Page direction has been reset and implemented.
- No backend API, schema, proxy, payment, auth, or production config changes were made.
- The page still only calls existing APIs.

Recently completed:
- Reworked `src/main/resources/static/index.html` first screen into:
  - left side: 主流程演示 with 初始化库存 -> 准备抢票请求 -> 请求凭证 -> 执行抢票实验;
  - right side: 证据面板 showing 抢票结果、库存变化、幂等判定、虚拟线程、requestId 变化、异步下单、扣减方案;
  - advanced details: 高级参数和策略对比, with Redis Lua / Redis Lock / MySQL optimistic lock kept out of the main flow.
- After user feedback that the local page still looked too similar, tightened the first screen again:
  - main flow is now three large experiment buttons: 初始化库存、发起抢票、用新请求重复提交;
  - requestId and idempotentKey are shown as experiment credentials under the action buttons;
  - the evidence panel is now a vertical evidence list instead of the previous card grid.
- Changed duplicate-submit behavior:
  - first rush uses requestId A + idempotentKey X;
  - duplicate submit generates requestId B while reusing idempotentKey X;
  - evidence panel shows `A -> B`, 幂等通过, and 异步下单未重复触发.
- Removed the old slogan-style page headline `5 分钟证明一个高并发抢票链路`.
- Updated `StaticDemoConsoleHtmlTest`, `README.md`, `docs/demo-runbook.md`, and `docs/github-showcase.md`.
- Regenerated all desktop README screenshots under `docs/screenshots/desktop/*.png`.

Verified latest:
- Inline JavaScript syntax check: passed.
- With JDK 21: `mvn "-Dtest=StaticDemoConsoleHtmlTest" test`: 5 tests passed.
- With JDK 21: `mvn package -DskipTests`: passed.
- Docker app redeploy: `docker compose up -d --no-deps --force-recreate app`.
- `http://localhost:8080/actuator/health`: `UP`.
- Browser/CDP walkthrough at desktop size:
  - preload inventory -> rush ticket -> duplicate submit all worked;
  - first rush showed `1000 -> 999`, `Virtual Thread`, and async order triggered;
  - duplicate submit showed requestId A -> B, same idempotentKey, no repeated stock deduction, and async order not repeated;
  - no horizontal overflow.
- Browser/CDP advanced checks:
  - Elasticsearch smoke search showed `命中数量 2`;
  - executor benchmark produced throughput;
  - mobile width check showed no horizontal overflow and no offscreen elements.
- Regenerated all 6 desktop screenshots again after the second visual pass; all are `1440x900`.

Next step only:
- Run final diff/status checks, commit the second visual pass, then stop unless the user asks for another TicketRush scope.

## Latest Snapshot - 2026-06-22 UI Direction Pause

Current goal:
- Pause TicketRush demo-page coding and rethink the page direction with the user before making more changes.

Current stage:
- The latest UI iterations were not accepted by the user.
- Do not continue patching the current page by inertia.
- Next window should first align on what the page must make visible and what should be removed or hidden.

User feedback to preserve:
- The page still feels messy and not meaningfully different.
- It should not rely on slogans such as "证明：高并发抢票不超卖、重复请求不重复扣库存".
- The page should let people see the system's core value through the demo process itself.
- The current behavior where request ID appears unchanged makes the demo feel fake or confusing.
- The user is asking whether a new window is needed because the direction is getting worse.

Important next-step guidance:
- Start the next window by reading this file, `PROJECT.md`, `SPEC.md`, `AGENTS.md`, `README.md`, current `git status`, and recent commits.
- Do not immediately code.
- First propose a simplified product/storyboard direction in plain Chinese:
  - what the first screen should show;
  - what operation sequence should be visible;
  - how request ID / idempotency should behave;
  - what evidence should be shown without dumping raw JSON;
  - what functions should be hidden, collapsed, or removed from the main view.
- Only after user confirms the direction should code changes continue.

Current local git note:
- There are recent local commits ahead of origin for UI changes:
  - `1f5dc72 style: localize ticket demo console`
  - `1e58051 fix: clarify inventory strategy demo`
  - `81e8dbc style: focus ticket demo page`
- These commits may need to be revised, reverted, or replaced depending on the user's next confirmed direction.
- Do not push before the user reviews the direction.

## Latest Snapshot - 2026-06-22 Demo Console Chinese Workflow

Current goal:
- Make the TicketRush local demo page Chinese, business-oriented, and easier to understand from screenshots.
- Clarify why different inventory deduction strategies can show different remaining stock.
- Redesign the page so the first screen clearly shows the high-concurrency ticket-rush proof points instead of a loose API console.

Current stage:
- No backend feature expansion.
- The static demo console still only calls existing APIs.
- README desktop screenshots have been regenerated from the updated Chinese UI.

Recently completed:
- Updated `src/main/resources/static/index.html` from an English Demo Console to `TicketRush 高并发抢票演示台`.
- Reworked visible sections into a clearer flow: 系统健康检查、抢票主链路、票档检索读模型、虚拟线程压测对比、观测入口.
- Added visible high-concurrency capability tags: 防超卖、幂等请求、热点限流、Java 21 Virtual Threads、最终一致性.
- Localized buttons, fields, result titles, busy/error text, status badges, and select option labels.
- Updated `StaticDemoConsoleHtmlTest` to assert the Chinese business labels while preserving IDs and API paths.
- Updated `README.md`, `docs/demo-runbook.md`, and `docs/github-showcase.md` to match the new Chinese operation path.
- Regenerated all README desktop screenshots under `docs/screenshots/desktop/*.png`.
- Added an explicit inventory-source explanation in the rush workflow:
  - Redis Lua and Redis Lock share the same Redis preloaded inventory.
  - MySQL optimistic lock reads the MySQL inventory table and is a comparison path.
- Moved rush result badges upward so `是否接单`、`Redis/MySQL 剩余库存`、`库存来源`、`虚拟线程` are visible in the main rush panel.
- Reworked the page from a broad console into a focused 5-minute demo:
  - first screen highlights 防超卖、幂等、限流、虚拟线程、异步削峰;
  - left side shows the step-by-step operation path;
  - right side shows human-readable conclusion cards;
  - raw JSON is collapsed under `查看原始接口响应`;
  - Elasticsearch, observability, and health checks are secondary sections.
- Added `3 重复提交验证幂等` to show duplicate requests do not deduct inventory again.

Verified latest:
- Inline JavaScript syntax check: passed.
- With JDK 21: `mvn "-Dtest=StaticDemoConsoleHtmlTest" test`: 5 tests passed.
- With JDK 21: `mvn package -DskipTests`: passed.
- Docker app redeploy: `docker compose up -d --no-deps --force-recreate app`.
- `http://localhost:8080/actuator/health`: `UP`.
- `http://localhost:8080/`: contains `5 分钟证明一个高并发抢票链路`, `本次演示结论`, and `重复提交验证幂等`.
- Browser walkthrough against local backend: health, initialize inventory, rush, duplicate-submit idempotency, index/search, and executor benchmark all produced visible results.
- Screenshot dimension check: all `docs/screenshots/desktop/*.png` are `1440x900`.
- Browser layout check: no horizontal overflow at the desktop screenshot viewport.
- `git diff --check`: passed.

Next step only:
- Commit this UI/demo-doc slice, then stop unless the user asks for another TicketRush scope.

## Latest Snapshot - 2026-06-21 Desktop Screenshot Showcase

Current goal:
- Replace the GitHub README screenshots with desktop landscape screenshots that cover each local Demo Console step.

Current stage:
- No feature work.
- README now references a 6-step `1440x900` desktop screenshot sequence.

Recently completed:
- Added `docs/screenshots/desktop/ticketrush-01-demo-console-overview.png`.
- Added `docs/screenshots/desktop/ticketrush-02-system-health.png`.
- Added `docs/screenshots/desktop/ticketrush-03-inventory-preload.png`.
- Added `docs/screenshots/desktop/ticketrush-04-rush-ticket.png`.
- Added `docs/screenshots/desktop/ticketrush-05-ticket-search.png`.
- Added `docs/screenshots/desktop/ticketrush-06-executor-benchmark.png`.
- Updated README `界面截图` to show the desktop step sequence.
- Updated `SPEC.md` screenshot progress wording.

Verified latest:
- Image dimension check for `docs/screenshots/desktop/*.png`: all screenshots are `1440x900`.
- Public wording scan across `README.md`, `SPEC.md`, `HANDOFF.md`, and `docs`: no matches for private-prep phrasing.
- All Markdown links across the repository: passed.
- `git diff --check`: passed with line-ending warnings only.

Next step only:
- Run docs checks, commit, push, then stop. Do not expand features.

## Latest Snapshot - 2026-06-21 Public Wording Cleanup

Current goal:
- Keep TicketRush public GitHub materials focused on technical demo, architecture, benchmarks, verification, and project boundaries.

Current stage:
- No feature work.
- Public-facing wording has been sanitized so the repository no longer reads like private notes.

Recently completed:
- Renamed the public demo guide to `docs/demo-runbook.md`.
- Replaced public wording such as private prep phrasing, scripted wording, and private-note wording with neutral technical demo language.
- Updated README, SPEC, HANDOFF, showcase docs, and related benchmark notes to point to the neutral document name.

Verified latest:
- Sensitive public-prep keyword scan across `README.md`, `SPEC.md`, `HANDOFF.md`, and `docs`: no matches.
- All Markdown links across the repository: passed.
- `git diff --check`: passed with line-ending warning only.

Next step only:
- Commit and push this public wording cleanup, then stop. Do not expand features.

## Latest Snapshot - 2026-06-21 GitHub Showcase Closeout

Current goal:
- Keep TicketRush as a stable GitHub-facing Java 21 high-concurrency ticket-rush showcase without adding new features.

Current stage:
- Core development is closed unless the user explicitly opens a new scope.
- GitHub presentation has been tightened with a top README showcase entry and a dedicated showcase note.

Recently completed:
- Added `docs/github-showcase.md` with quick highlights, recommended browsing order, local demo flow, verification evidence, project summary wording, and no-overclaim boundaries.
- Added a `GitHub 展示入口` section near the top of `README.md`.
- Linked the showcase note from README documentation navigation.
- Marked the showcase note complete in `SPEC.md`.

Workspace status:
- Check with `git status --short --branch`.
- Use JDK 21 for Maven if Java verification is rerun.

Verified latest:
- Markdown links in `README.md` and `docs/github-showcase.md`: passed.
- Sensitive pattern scan for private keys and common token formats in changed docs: no matches.
- `git diff --check`: passed.

Next step only:
- Run docs checks, commit this GitHub showcase closeout, then stop. Do not expand features.

## Latest Snapshot - 2026-06-18 Demo Prep Closeout

Current goal:
- Keep TicketRush stable as a local runnable, benchmarkable, demo-ready Java 21 high-concurrency ticket-rush system.
- Prepare the user for tomorrow's demo familiarization without adding new product features.

Current stage:
- Core development is closed unless the user explicitly opens a new scope.
- Demo Console is complete at `http://localhost:8080/`.
- Demo materials are now prepared for tomorrow's familiarization.

Recently completed:
- Added `docs/demo-runbook.md` with TicketRush project overview notes, Demo Console walkthrough, CLI fallback, high-frequency questions, and no-overclaim boundaries.
- Added the runbook to README documentation navigation.
- Added workspace-level `INTERVIEW_STUDY_PLAN.md` at `E:\project\work\job\INTERVIEW_STUDY_PLAN.md` to coordinate SmartKB and TicketRush study order.

Workspace status:
- Check with `git status --short --branch`.
- Use JDK 21 for Maven if any Java verification is rerun.

Verified latest:
- Markdown links in `README.md` and `docs/demo-runbook.md`: passed.
- `git diff --check`: passed.
- With JDK 21: `mvn test`: 52 tests passed.
- Docker runtime health: `http://localhost:8080/actuator/health` returned `UP` with MySQL, Redis, RocketMQ binder, Elasticsearch, Sentinel, and Nacos discovery available.
- `GET http://localhost:8080/` served the Demo Console and expected controls (`preloadButton`, `rushButton`, Ticket Search, Executor Benchmark).

Next step only:
- Commit and push this documentation-only demo-prep slice, then stop. Do not expand features.

## Previous Snapshot - 2026-06-18 Demo Console

Current goal:
- Keep TicketRush as a local runnable, benchmarkable, demo-ready Java 21 high-concurrency ticket-rush system.
- Add the missing page needed for demo walkthroughs without expanding into a full frontend/admin system.

Current stage:
- Docker Compose full stack, k6 benchmark reports, Virtual Threads benchmark, Sentinel/Redis governance, Prometheus evidence, hotspot-spread comparison, Seata AT demo, Elasticsearch search, and the local Demo Console are complete.
- Demo Console is available at `http://localhost:8080/`.
- The page only calls existing APIs: health, inventory preload, rush ticket, Elasticsearch index/search, executor benchmark, and ops links.

Recently completed:
- Added `src/main/resources/static/index.html` as a lightweight TicketRush Demo Console.
- Added `StaticDemoConsoleHtmlTest` to guard static entry points, API paths, responsive layout guards, and unique HTML IDs.
- Updated README/SPEC to document the local demo page and keep the scope boundary explicit.

Workspace status:
- Check with `git status --short --branch`.
- Use JDK 21 for Maven; the default shell may point to JDK 22 and fail the enforcer rule.
- Known JDK 21 path on this machine: `C:\Users\xz\.antigravity\extensions\redhat.java-1.54.0-win32-x64\jre\21.0.10-win32-x86_64`.

Verified latest:
- With JDK 21: `mvn "-Dtest=StaticDemoConsoleHtmlTest" test`: 4 tests passed.
- Inline JS syntax check with Node: passed.
- With JDK 21: `mvn test`: 52 tests passed.
- With JDK 21: `mvn package -DskipTests`: passed.
- Docker Compose app restart: `docker compose up -d --no-deps --force-recreate app`; app health became `UP`.
- `GET http://localhost:8080/` served the Demo Console and expected controls/API paths.
- Runtime API smoke:
  - `GET /api/system/health`: `success=true`, `status=UP`, Java 21, virtual threads enabled.
  - `POST /api/benchmark/executors`: `success=true`, `mode=VIRTUAL_THREAD`, `virtualThreadTaskCount=100`.
  - `POST /api/rush/inventory/preload`: `success=true`.
  - `POST /api/rush/tickets`: `success=true`, `accepted=true`, `remainingStock=999`, `processedByVirtualThread=true`.
  - `POST /api/search/events/9101781814509/index`: `indexedSkuCount=2`.
  - `GET /api/search/ticket-skus?...Codex%20Smoke...`: `total=2`.
- Browser smoke:
  - Chromium headless screenshots generated under `target/demo-console-desktop.png` and `target/demo-console-mobile.png`.
  - Chrome DevTools Protocol layout check found no horizontal overflow on desktop/mobile viewports.

Not verified latest:
- Playwright package-level browser smoke was not used because the Chromium headless-shell cache was incomplete and `npx playwright install chromium` timed out. Browser verification used the cached Chromium executable directly instead.
- Nacos may still log local gRPC reconnect noise in Docker, but app health remains `UP` and the demo flows work.

Next step only:
- Commit and push this demo-console slice, then stop. Do not start another feature unless explicitly requested.

## 2026-06-18 Work Log - Demo Console

Current goal:
- Add the missing TicketRush page needed for demo walkthroughs, without expanding the product scope.

Completed:
- Added a static Demo Console at `/`.
- The page covers system health, rush inventory preload, ticket rush, Elasticsearch indexing/search, executor benchmark, and operational links.
- The page intentionally reuses existing APIs only and does not add auth, admin CRUD, payment, or order-management UI.
- Added a focused HTML regression test for static IDs, responsive guards, API path strings, and core JS function names.
- Updated README, SPEC, and HANDOFF.

Modified files:
- `README.md`
- `SPEC.md`
- `HANDOFF.md`
- `src/main/resources/static/index.html`
- `src/test/java/com/ticketrush/StaticDemoConsoleHtmlTest.java`

Verified:
- With JDK 21: `mvn "-Dtest=StaticDemoConsoleHtmlTest" test`: 4 tests passed.
- Inline JS syntax check with Node: passed.
- With JDK 21: `mvn test`: 52 tests passed.
- With JDK 21: `mvn package -DskipTests`: passed.
- Docker app restarted and `/actuator/health` became `UP`.
- `GET /` served the Demo Console.
- `GET /api/system/health`, `POST /api/benchmark/executors`, `POST /api/rush/inventory/preload`, `POST /api/rush/tickets`, `POST /api/search/events/9101781814509/index`, and `GET /api/search/ticket-skus` smoke checks passed.
- Chromium desktop/mobile screenshots were generated under `target/`, and a DevTools layout check found no horizontal overflow.

Not verified:
- Playwright package-level browser smoke, because browser install timed out. Direct cached-Chromium screenshot and CDP layout checks passed instead.

Next step:
- Commit and push this slice, then stop.

## Previous Snapshot - 2026-06-18 Elasticsearch Runtime Smoke

Current goal:
- Keep TicketRush as a local runnable, benchmarkable, demo-ready Java 21 high-concurrency ticket-rush system.

Current stage:
- Docker Compose full stack, k6 benchmark reports, Virtual Threads benchmark, Sentinel/Redis governance, Prometheus evidence, hotspot-spread comparison, Seata AT demo, and Elasticsearch activity/SKU search code are complete.
- Elasticsearch is a read-side model only; the rush write path remains Sentinel -> Redis admission -> inventory deduction -> RocketMQ async order -> timeout compensation.
- Elasticsearch runtime smoke is now verified against Docker Compose with MySQL seed data and the local Elasticsearch container.

Recently completed:
- Added `TicketSearchApplicationService`, `TicketSearchRepository`, and an Elasticsearch adapter backed by `ElasticsearchOperations`.
- Added `POST /api/search/events/{eventId}/index` to rebuild search documents from existing MySQL event/SKU data.
- Added `GET /api/search/ticket-skus` with keyword, event ID, event status, SKU status, and pagination filters.
- Added `docs/elasticsearch-search.md` plus README/SPEC updates.
- Fixed runtime search after smoke found that date-only Elasticsearch `_source` values could not be mapped back to `LocalDateTime`.

Workspace status:
- Check with `git status --short --branch`.
- Use JDK 21 for Maven; the default shell may point to JDK 22 and fail the enforcer rule.
- Known JDK 21 path on this machine: `C:\Users\xz\.antigravity\extensions\redhat.java-1.54.0-win32-x64\jre\21.0.10-win32-x86_64`.

Verified latest:
- With JDK 21: `mvn "-Dtest=TicketSearchApplicationServiceTest,ElasticsearchTicketSearchRepositoryTest" test`: 8 tests passed.
- With JDK 21: `mvn test`: 48 tests passed.
- `git diff --check`: passed.
- With JDK 21: `mvn package -DskipTests`: passed.
- Docker Compose app restart: `docker compose up -d --no-deps --force-recreate app`: app health became `UP`.
- Runtime smoke:
  - `POST /api/search/events/9101781814509/index`: `success=true`, `indexedSkuCount=2`.
  - `GET /api/search/ticket-skus?keyword=Codex%20Smoke&eventId=9101781814509&eventStatus=SELLING&skuStatus=ON_SALE&page=0&size=10`: `success=true`, `total=2`.
  - ES `_source` now stores LocalDateTime values as ISO date-time strings while still using ES date mapping.

Not verified latest:
- No known gap for the Elasticsearch smoke slice. Nacos still logs local gRPC reconnect noise in Docker, but app health remains `UP` and search works.

Next step only:
- Commit and push this runtime fix, then stop. Do not start another feature unless explicitly requested.

## 2026-06-18 Work Log - Elasticsearch Runtime Smoke Fix

Current goal:
- Finish the Elasticsearch activity/SKU search slice by making the runtime Docker smoke pass.

Problem found:
- `GET /api/search/ticket-skus` returned HTTP 500 only when the query had hits.
- Empty-hit queries returned 200, and direct Elasticsearch `_search` showed the expected two documents.
- Root cause: Spring Data Elasticsearch could return date-only `_source` values such as `2026-06-25` for `LocalDateTime` fields, which failed when mapping hits back to the document record.

Completed:
- Marked the production `TicketSearchApplicationService` constructor with `@Autowired` so Spring chooses it instead of the test-only constructor.
- Changed `TicketSearchDocument` date fields to string-backed ES date fields and parse them explicitly back into the domain record.
- Kept compatibility with both full ISO date-time strings and older date-only demo documents.
- Added a regression test for date-only Elasticsearch source values.

Modified files:
- `src/main/java/com/ticketrush/application/service/TicketSearchApplicationService.java`
- `src/main/java/com/ticketrush/infrastructure/elasticsearch/TicketSearchDocument.java`
- `src/test/java/com/ticketrush/infrastructure/elasticsearch/ElasticsearchTicketSearchRepositoryTest.java`
- `HANDOFF.md`

Verified:
- With JDK 21: `mvn "-Dtest=TicketSearchApplicationServiceTest,ElasticsearchTicketSearchRepositoryTest" test`: 8 tests passed.
- With JDK 21: `mvn test`: 48 tests passed.
- `git diff --check`: passed.
- With JDK 21: `mvn package -DskipTests`: passed.
- Docker app health: `UP`.
- Runtime search smoke returned `total=2` for the seeded `Codex Smoke` event.

Next step:
- Commit and push this fix, then wait for user direction.

## 2026-06-18 Work Log - Elasticsearch Activity/SKU Search

Current goal:
- Add the next TicketRush slice: Elasticsearch activity/SKU query integration, without changing the proven rush write path.

Completed:
- Added a domain search port with `TicketSearchRecord`, `TicketSearchQuery`, `TicketSearchPage`, and `TicketSearchRepository`.
- Added `TicketSearchApplicationService` to rebuild search records from existing MySQL `TicketEvent` and `TicketSku` data.
- Added Elasticsearch infrastructure with `TicketSearchDocument` and `ElasticsearchTicketSearchRepository`.
- Added `POST /api/search/events/{eventId}/index`.
- Added `GET /api/search/ticket-skus`.
- Added unit coverage for application mapping, query normalization, Elasticsearch query JSON, index creation, and document mapping.
- Added `docs/elasticsearch-search.md` and updated README/SPEC/HANDOFF.

Modified files:
- `src/main/java/com/ticketrush/application/command/IndexTicketEventCommand.java`
- `src/main/java/com/ticketrush/application/command/TicketSearchCommand.java`
- `src/main/java/com/ticketrush/application/dto/TicketEventIndexResult.java`
- `src/main/java/com/ticketrush/application/dto/TicketSearchResult.java`
- `src/main/java/com/ticketrush/application/service/TicketSearchApplicationService.java`
- `src/main/java/com/ticketrush/domain/model/TicketSearchPage.java`
- `src/main/java/com/ticketrush/domain/model/TicketSearchQuery.java`
- `src/main/java/com/ticketrush/domain/model/TicketSearchRecord.java`
- `src/main/java/com/ticketrush/domain/repository/TicketSearchRepository.java`
- `src/main/java/com/ticketrush/infrastructure/elasticsearch/ElasticsearchTicketSearchRepository.java`
- `src/main/java/com/ticketrush/infrastructure/elasticsearch/TicketSearchDocument.java`
- `src/main/java/com/ticketrush/interfaces/controller/TicketSearchController.java`
- `src/main/java/com/ticketrush/interfaces/response/TicketEventIndexResponse.java`
- `src/main/java/com/ticketrush/interfaces/response/TicketSearchItemResponse.java`
- `src/main/java/com/ticketrush/interfaces/response/TicketSearchResponse.java`
- `src/test/java/com/ticketrush/application/service/TicketSearchApplicationServiceTest.java`
- `src/test/java/com/ticketrush/infrastructure/elasticsearch/ElasticsearchTicketSearchRepositoryTest.java`
- `docs/elasticsearch-search.md`
- `README.md`
- `SPEC.md`
- `HANDOFF.md`

Verified:
- With JDK 21: `mvn "-Dtest=TicketSearchApplicationServiceTest,ElasticsearchTicketSearchRepositoryTest" test`: 7 tests passed.
- With JDK 21: `mvn test`: 47 tests passed.
- `git diff --check`: passed.

Not verified:
- Runtime Elasticsearch smoke against Docker Compose. It needs a running stack and MySQL event/SKU seed data.

Next step:
- Commit and push this slice. Do not start another feature unless explicitly requested.

## 当前目标

把 TicketRush 收口为可本地运行、可压测、可设计说明的 Java 21 高并发票务秒杀系统。

它在求职叙事中的定位：

- 证明 8 年 Java 后端基本盘：高并发、Redis、RocketMQ、MySQL、Sentinel、幂等、一致性、监控和部署。
- 作为 SmartKB/Agent 工程平台的真实 Java 项目样本，用于后续验证项目接管、代码上下文检索、任务规划和 eval 能力。

## 当前阶段

Docker Compose 全链路启动、第一轮 Dockerized k6 本地压测、Virtual Threads vs 传统线程池报告、稳定性治理 before/after 对照、Prometheus/Grafana 指标证据、多票档热点分摊对比、Seata AT 示例已完成。下一步是 Elasticsearch 活动/票档查询集成。

## 已完成

- Java 21 + Spring Boot 3 项目骨架。
- 统一响应、错误码、全局异常处理。
- Virtual Threads 配置和执行器对比接口。
- 票务活动、票档、库存、订单领域模型。
- Redis Lua 原子扣减、防超卖和幂等。
- Redis 分布式锁扣减方案。
- MySQL 乐观锁扣减方案。
- 抢票核心接口 `/api/rush/tickets`。
- 本地库存预热接口 `/api/rush/inventory/preload`。
- RocketMQ 异步创建订单链路。
- 订单消费幂等、超时关闭、库存释放补偿。
- Sentinel 全局限流、热点参数限流、限流兜底响应。
- Redis 准入令牌和热点库存自动预热。
- k6 压测脚本和稳定性治理文档模板。
- Dockerized k6 三种库存策略低负载 baseline 报告。
- Dockerized k6 单热点票档默认治理观察记录。
- Virtual Threads vs 传统线程池执行器 benchmark 报告。
- Dockerized k6 稳定性治理 before/after 对照报告。
- Prometheus/Grafana 压测指标证据报告。
- Dockerized k6 多票档热点分摊对比报告。
- Seata AT 模式 MySQL 库存预占 + 订单落库示例。
- Prometheus 配置、Grafana 说明、Arthas 诊断案例、Kubernetes/K3s 部署清单。
- README、架构图、数据库 schema、踩坑记录等求职展示文档。
- **Docker Compose 全链路一键启动**（app + 9 中间件）。
- **RocketMQ Broker 容器启动修复**（NPE 根因：Docker named volume 权限 → bind mount + entrypoint 脚本）。
- **application-docker.yml** profile（Docker 内部服务名覆盖）。
- **JDK 21 下 mvn clean verify 通过**（35 tests, 0 failures）。
- **Docker 全链路验证通过**：health UP、抢票成功、虚拟线程生效、Prometheus 指标可抓取。

## 本次已修改文件

- `Dockerfile` — 简化为单阶段（本地 mvn package 后挂载 JAR）
- `docker-compose.yml` — 添加 app 服务、修复 broker、清理 named volume
- `docker/prometheus/prometheus.yml` — scrape 目标改为 `app:8080`
- `docker/rocketmq/broker.conf` — 补充 storePathRootDir 等路径（参考用）
- `docker/rocketmq/broker-entrypoint.sh` — **新建**，修复 Broker NPE
- `src/main/resources/application-docker.yml` — **新建**，Docker 内部服务名
- `.gitignore` — 添加 `docker/rocketmq/store/`
- `SPEC.md` — 更新验证勾选
- `HANDOFF.md` — 更新本文件

## 本次已验证

- `mvn clean verify`：35 tests, 0 failures, 0 errors, BUILD SUCCESS (JDK 21.0.7 JBR)
- `docker compose up -d`：10/10 容器运行正常
- `/api/system/health`：UP, virtualThreadsEnabled=true, JDK 21.0.11
- `/api/rush/inventory/preload`：库存预热成功 (skuId=1001, totalStock=1000)
- `/api/rush/tickets`：抢票成功 (accepted=true, remainingStock=999, virtualThread=true)
- `/actuator/prometheus`：指标正常输出

## 下一步

1. 补 Elasticsearch 活动/票档查询集成。
2. 按需做更高 VUS 下多票档分摊与全局限流边界观察。
3. 按需补 Seata AT 真实 Docker MySQL + undo_log 联调记录。

## 未验证

- Elasticsearch 集成未实现。
- 更高 VUS 下多票档分摊与全局限流共同生效的边界尚未观察。
- Seata AT 示例已通过单元测试，尚未做真实 Docker MySQL + undo_log 联调。

## 风险和注意事项

- 不要提交 RocketMQ、MySQL、Redis、Elasticsearch 等本地运行数据目录。
- `docker/rocketmq/store/` 已加入 `.gitignore`，不要提交。
- 不要修改数据库 schema 或迁移文件，除非用户明确要求。
- 不要把密钥、真实账号、token、cookie、私钥或 `.env` 内容写入仓库。
- 不要为了补齐技术点继续无限扩展功能，TicketRush 当前优先级是收口验证和真实压测。

## 2026-06-17 Work Log - GitHub Safety Cleanup

Current goal:
- Make the pushed GitHub version safer as a private backup and easier to turn into a public showcase later.

Completed:
- Confirmed the working tree was clean before the safety pass.
- Confirmed `.env` is ignored.
- Reviewed tracked public-facing files for Secret/env/token/local-path indicators without printing secret values.
- Changed `deploy/k8s/secret.yaml` from demo-looking database values to explicit `CHANGE_ME_*` placeholders.
- Updated `deploy/k8s/README.md` to state that real database credentials must be filled locally and must not be committed.
- Checked unauthenticated GitHub API visibility; the repository returned 404, so it is not visible as a public repository from that view.

Modified files:
- `deploy/k8s/secret.yaml`
- `deploy/k8s/README.md`
- `HANDOFF.md`

Verified:
- `git diff --check`: only LF/CRLF warnings, no whitespace errors.
- `docker compose config --quiet`: passed.
- `npx --yes js-yaml deploy/k8s/secret.yaml deploy/k8s/kustomization.yaml deploy/k8s/deployment.yaml`: passed.

Not verified:
- Maven tests were not rerun for TicketRush because this step only changed K8s/documentation files.

Next step:
- Commit and push the safety cleanup, then continue README/showcase polishing or k6 pressure-test reporting.

## 2026-06-17 Work Log - README Showcase Polish

Current goal:
- Turn TicketRush `README.md` into a GitHub/project-showcase entry page that pairs cleanly with SmartKB.

Completed:
- Rewrote the README around the high-concurrency ticket-rush main path:
  - Sentinel guard -> Redis admission token -> Java 21 Virtual Threads -> inventory strategy -> RocketMQ async order -> timeout compensation.
- Added a Mermaid architecture diagram, project highlights, technology table, feature checklist, Docker Compose startup path, manual smoke commands, k6 entry points, API overview, verification status, project structure, technical explanation notes, documentation navigation, and safety notes.
- Kept Seata and Elasticsearch described as reserved/pending integration rather than completed business features.
- Marked README showcase polish complete in `SPEC.md`.

Modified files:
- `README.md`
- `SPEC.md`
- `HANDOFF.md`

Verified:
- README local link scan via PowerShell: passed (`README links ok: 21`).
- Public-safety scan for private gateways, token patterns, private keys, and local absolute paths: no matches.
- `git diff --check`: passed with no whitespace errors.
- Maven tests were not rerun because this step only changed documentation.

Next step:
- Continue with Prometheus/Grafana metric evidence or Seata example.

## 2026-06-17 Work Log - k6 Benchmark Report

Current goal:
- Produce the first real local k6 benchmark record for TicketRush.

Completed:
- Confirmed the working tree was clean before the benchmark task.
- Confirmed Docker Compose stack was already running.
- Confirmed `/api/system/health` returned `UP`, Java 21.0.11, `virtualThreadsEnabled=true`, and `currentThreadVirtual=true`.
- Used Dockerized k6 (`grafana/k6`) because k6 was not installed on the host.
- Fixed k6 scripts so expected business statuses `409/429/503` are not counted as transport failures.
- Ran low-load baseline for `REDIS_LUA`, `REDIS_LOCK`, and `MYSQL_OPTIMISTIC_LOCK`.
- Seeded local Docker MySQL with benchmark event/sku/inventory rows for the MySQL optimistic-lock strategy.
- Ran guarded hot-sku stability observation with `VUS=30`, `DURATION=20s`, `SKU_SPREAD=1`.
- Added `docs/rush-benchmark-report.md` and updated `docs/stability-benchmark.md`.
- Updated README and SPEC to reflect the new benchmark report.

Key results:
- `REDIS_LUA`: 45.40 req/s, p95 4.09ms, 0.00% HTTP failed.
- `REDIS_LOCK`: 44.26 req/s, p95 5.72ms, 0.00% HTTP failed.
- `MYSQL_OPTIMISTIC_LOCK`: 39.15 req/s, p95 7.98ms, 0.00% HTTP failed, one 3.9s max-latency outlier.
- Guarded hot-sku run: 833,629 requests, 35,765.45 req/s, 2,023 accepted, 831,605 rate-limited, 0.00% unexpected responses.

Modified files:
- `README.md`
- `SPEC.md`
- `HANDOFF.md`
- `docs/rush-benchmark-report.md`
- `docs/stability-benchmark.md`
- `scripts/k6/rush-ticket.js`
- `scripts/k6/stability-governance.js`

Verified:
- Dockerized k6 baseline runs passed thresholds for all three inventory strategies.
- Dockerized k6 guarded hot-sku run passed thresholds with 0.00% unexpected responses.
- Raw k6 summary JSON was generated under `target/k6/` and was not intended for git.

Not verified:
- Stability before/after comparison with governance disabled or threshold-tuned.
- Virtual Threads vs traditional thread pool benchmark report.
- Prometheus/Grafana metric screenshots or exports during the k6 runs.

Next step:
- Continue with Prometheus/Grafana metric evidence or Seata example.

## 2026-06-17 Work Log - Governance Comparison Report

Current goal:
- Produce a stability-governance before/after comparison report.

Completed:
- Confirmed the working tree was clean before the task.
- Read Sentinel and Redis admission guard implementations.
- Confirmed Sentinel disabled mode means rules are not loaded, while the guard still allows traffic.
- Confirmed Redis admission can be disabled via `TICKETRUSH_RUSH_ADMISSION_ENABLED=false`.
- Started a temporary `ticketrush-app-noguard` container on the same Docker network with:
  - `TICKETRUSH_SENTINEL_ENABLED=false`
  - `TICKETRUSH_RUSH_ADMISSION_ENABLED=false`
- Ran default protected hot-sku k6 comparison against `http://app:8080`.
- Ran no-guard hot-sku k6 comparison against `http://ticketrush-app-noguard:8080`.
- Removed the temporary `ticketrush-app-noguard` container after the run.
- Added `docs/governance-comparison-report.md`.
- Updated README, SPEC, HANDOFF, and `docs/stability-benchmark.md`.

Key results:
- Protected: 8,708 requests, 741.53 req/s, 1,197 accepted, 7,510 rate-limited, 86.25% rate-limit ratio, p95 3.21ms, max 16.52ms.
- No Guard: 7,502 requests, 724.48 req/s, 7,501 accepted, 0 rate-limited, p95 4.94ms, max 350.89ms.
- Both runs had 0.00% unexpected responses and 0.00% HTTP failed.

Modified files:
- `README.md`
- `SPEC.md`
- `HANDOFF.md`
- `docs/governance-comparison-report.md`
- `docs/stability-benchmark.md`

Verified:
- Dockerized k6 protected and no-guard runs passed thresholds.
- Main `ticketrush-app` health remained `UP` after the comparison.
- Raw k6 summary JSON was generated under `target/governance-comparison/` and was not intended for git.

Not verified:
- Prometheus/Grafana metric screenshots or exports during the comparison.
- Multi-SKU spread comparison with `SKU_SPREAD > 1`.

Next step:
- Add Prometheus/Grafana metric evidence or move to the Seata example.

## 2026-06-18 Work Log - Observability Benchmark Evidence

Current goal:
- Add Prometheus/Grafana metric evidence for TicketRush benchmark runs.

Completed:
- Confirmed the working tree was clean before the task.
- Confirmed Prometheus targets were healthy:
  - `ticketrush-app`: `app:8080/actuator/prometheus`, health `up`.
  - `prometheus`: `localhost:9090/metrics`, health `up`.
- Confirmed Prometheus scrape interval is 15s.
- Confirmed Grafana `TicketRush Overview` dashboard includes HTTP RPS, HTTP p95, Process CPU, JVM Heap Used, and JVM Live Threads panels.
- Ran a 60s Dockerized k6 protected hot-sku run for Prometheus collection:
  - `VUS=10`, `DURATION=60s`, `SKU_SPREAD=1`, `SLEEP=0.01`, `STRATEGY=REDIS_LUA`.
- Exported Prometheus query-range metrics for HTTP RPS, accepted RPS, rate-limited RPS, p95, CPU, heap, live threads, Hikari active/pending, and Redis command completion rate.
- Added `docs/observability-benchmark-report.md`.
- Updated `docs/observability.md` to match the current Docker Compose Prometheus target `app:8080`.
- Updated README and SPEC to reflect Prometheus/Grafana metric evidence.

Key results:
- k6: 52,173 requests, 838.88 req/s, 7,167 accepted, 45,005 rate-limited, p95 3.24ms, 0.00% unexpected responses.
- Prometheus: total RPS max 828.63/s, accepted RPS max 114.71/s, rate-limited RPS max 713.92/s.
- Prometheus HTTP p95 max about 0.0031s.
- Process CPU max about 0.0215, heap max 466,801,296 bytes, live threads 480-481.
- Hikari active and pending stayed at 0, which matches the Redis Lua + entry-guard scenario.

Modified files:
- `README.md`
- `SPEC.md`
- `HANDOFF.md`
- `docs/observability.md`
- `docs/observability-benchmark-report.md`

Verified:
- Dockerized k6 run passed thresholds.
- Prometheus API target status showed `ticketrush-app` as healthy.
- Prometheus query-range exports returned non-empty samples for all report metrics.
- Raw k6/Prometheus exports were generated under `target/prometheus-evidence/` and were not intended for git.

Not verified:
- Grafana screenshot export; this step uses Prometheus API data as reproducible evidence.
- Container-level CPU/memory/network metrics for MySQL, Redis, and RocketMQ.

Next step:
- Move to Seata example, or run multi-SKU hotspot-spread comparison.

## 2026-06-17 Work Log - Executor Benchmark Report

Current goal:
- Produce the Virtual Threads vs traditional thread pool benchmark report for TicketRush.

Completed:
- Confirmed the working tree was clean before the benchmark task.
- Read the benchmark controller/service/request/response implementation.
- Confirmed traditional benchmark executor is a fixed 200-platform-thread pool.
- Confirmed `/api/system/health` returned `UP`, Java 21.0.11, `virtualThreadsEnabled=true`, and `currentThreadVirtual=true`.
- Ran warm-up calls for both executor modes.
- Ran pure I/O waiting benchmark: 5,000 tasks, 50ms blocking, 0 CPU tokens.
- Ran mixed benchmark: 2,000 tasks, 20ms blocking, 50 CPU tokens.
- Added `docs/executor-benchmark-report.md`.
- Updated README, SPEC, HANDOFF, and `docs/rush-benchmark-report.md`.

Key results:
- Pure I/O waiting: `VIRTUAL_THREAD` finished in 56ms at 89,285.71 tasks/s; `TRADITIONAL_THREAD_POOL` finished in 1,263ms at 3,958.83 tasks/s. Virtual-thread throughput was about 22.55x.
- Mixed I/O + CPU: `VIRTUAL_THREAD` finished in 83ms at 24,096.39 tasks/s; `TRADITIONAL_THREAD_POOL` finished in 215ms at 9,302.33 tasks/s. Virtual-thread throughput was about 2.59x.
- Virtual-thread runs used one virtual thread per task; traditional runs used 200 platform threads and queued the rest.

Modified files:
- `README.md`
- `SPEC.md`
- `HANDOFF.md`
- `docs/executor-benchmark-report.md`
- `docs/rush-benchmark-report.md`

Verified:
- `/api/benchmark/executors` returned successful responses for all four official benchmark runs.
- Raw benchmark JSON was generated under `target/executor-benchmark/` and was not intended for git.

Not verified:
- Prometheus/Grafana metric screenshots or exports during the executor benchmark.
- Stability multi-SKU spread comparison with `SKU_SPREAD > 1`.

Next step:
- Continue with Prometheus/Grafana metric evidence or Seata example.

## 2026-06-18 Work Log - Hotspot Spread Benchmark

Current goal:
- Produce a multi-SKU hotspot-spread comparison for TicketRush stability governance.

Completed:
- Confirmed the working tree was clean before the task.
- Confirmed `/api/system/health` returned `UP`, Java 21.0.11, `virtualThreadsEnabled=true`, and `currentThreadVirtual=true`.
- Confirmed Docker Compose services were running.
- Ran Dockerized k6 single-hotspot comparison with `SKU_SPREAD=1`, `VUS=10`, `DURATION=10s`, `SLEEP=0.01`, `STRATEGY=REDIS_LUA`.
- Ran Dockerized k6 multi-SKU comparison with `SKU_SPREAD=20` under the same VUS, duration, sleep, stock, and strategy.
- Added `docs/hotspot-spread-benchmark-report.md`.
- Updated README, SPEC, HANDOFF, and `docs/stability-benchmark.md`.

Key results:
- `SKU_SPREAD=1`: 8,724 requests, 871.47 req/s, 1,085 accepted, 7,638 rate-limited, 87.56% `C0429`, p95 3.23ms.
- `SKU_SPREAD=20`: 7,516 requests, 749.51 req/s, 7,496 accepted, 0 rate-limited, 0.00% `C0429`, p95 4.32ms.
- Both runs had 0.00% unexpected responses and 0.00% HTTP failed.

Modified files:
- `README.md`
- `SPEC.md`
- `HANDOFF.md`
- `docs/stability-benchmark.md`
- `docs/hotspot-spread-benchmark-report.md`

Verified:
- Dockerized k6 single-hotspot and multi-SKU runs passed thresholds.
- Raw k6 summary JSON was generated under `target/multi-sku-comparison/` and was not intended for git.

Not verified:
- Higher-VUS boundary where multi-SKU spread and global QPS limiting are both active.

Next step:
- Continue with Seata example or Elasticsearch activity/SKU query integration.

## 2026-06-18 Work Log - Seata Transaction Demo

Current goal:
- Add a scoped Seata distributed-transaction example without changing the proven Redis/RocketMQ main rush path.

Completed:
- Added `SeataOrderTransactionDemoService`.
- The example uses `@GlobalTransactional(name = "ticketrush-seata-mysql-rush-order", rollbackFor = Exception.class)`.
- The example only allows `MYSQL_OPTIMISTIC_LOCK`, then reserves MySQL inventory and creates a `PENDING` order in one global transaction.
- Kept Redis Lua and RocketMQ on the existing final-consistency path.
- Added `docs/seata-transaction-demo.md`.
- Updated README, SPEC, HANDOFF, and `docs/final-consistency.md`.

Modified files:
- `README.md`
- `SPEC.md`
- `HANDOFF.md`
- `docs/final-consistency.md`
- `docs/seata-transaction-demo.md`
- `src/main/java/com/ticketrush/infrastructure/seata/SeataOrderTransactionDemoService.java`
- `src/test/java/com/ticketrush/infrastructure/seata/SeataOrderTransactionDemoServiceTest.java`

Verified:
- `mvn -Dtest=SeataOrderTransactionDemoServiceTest test`: 5 tests, 0 failures, 0 errors with JDK 21.0.7 JBR.

Not verified:
- Real Seata AT integration against Docker MySQL with `undo_log`; this task intentionally avoids modifying business schema.

Next step:
- Continue with Elasticsearch activity/SKU query integration.

## 接管开场模板

新窗口或换模型时，先执行：

```powershell
Get-Content -Raw HANDOFF.md
Get-Content -Raw PROJECT.md
Get-Content -Raw SPEC.md
git status --short
git log --oneline -5
```

然后先输出：

```text
当前目标：
当前阶段：
已完成：
未完成：
工作区是否有未提交改动：
我下一步只做：
```
