# Credit Sentinel — backend deep dive

A study guide for the server. Before an interview you should be able to say what each piece does, how the pieces connect, and why that shape was chosen instead of the alternative.

Companion file: `docs/FINANCE_GUIDE.md` is the credit vocabulary. This file is the Java.

---

## Contents

1. [The 10,000-foot view](#1-the-10000-foot-view)
2. [Why one process](#2-why-one-process)
3. [Package map and the path from the button](#3-package-map-and-the-path-from-the-button)
4. [Schema, table by table](#4-schema-table-by-table)
5. [Breach detection](#5-breach-detection)
6. [The agent loop](#6-the-agent-loop)
7. [The six tools](#7-the-six-tools)
8. [Citation check](#8-citation-check)
9. [HTTP routes and pages](#9-http-routes-and-pages)
10. [Audit rows and metrics](#10-audit-rows-and-metrics)
11. [Two end-to-end traces](#11-two-end-to-end-traces)
12. [Design choices and the alternative that was rejected](#12-design-choices-and-the-alternative-that-was-rejected)
13. [Known weaknesses](#13-known-weaknesses)
14. [Likely interview questions](#14-likely-interview-questions)

---

## 1. The 10,000-foot view

One Spring Boot process. Postgres. A Thymeleaf desk at `/`. The same actions exist under `/api`.

```
startup
  Flyway loads the book and one historical Pinnacle case
  BreachScanService walks every facility
  BreachDetector returns breaches
  two credit_alert rows are inserted: Northwind LIMIT_UTILIZATION, Harbor COVENANT

analyst clicks Run investigation
  InvestigationService opens or reuses a credit_case
  CreditReviewAgent runs at most 6 model turns
  the model asks for tools; Java executes them; results are appended
  the model returns a JSON memo
  CitationVerifier rejects numbers that no tool returned
  case status becomes PENDING_APPROVAL

analyst records APPROVE or REJECT
  DecisionService writes human_decision
  case becomes APPROVED or REJECTED
  the alert is closed
  facility.committed_limit is not updated
```

There is no message bus, no second service, and no write path back to a core banking system. The facility row is seed data plus reads.

Default LLM mode is `scripted`. `ScriptedCreditChatModel` implements the same `ChatModel` interface a live OpenAI model implements. The loop, the tools, and the verifier do not have a scripted branch.

---

## 2. Why one process

The portfolio scope was one service you can start with Docker Compose and explain in ten minutes. Detection, the agent, and the desk share one transaction and one database.

What splitting them would buy:

- The agent could scale separately from the read API.
- A model outage would not take down the book page.
- A different team could own the rules engine.

Why this repo does not do it:

- There is one analyst and eight borrowers. A network hop would add failure modes and no operational benefit.
- The interesting control is inside the process: rules do not call the model, and the decision service does not call the model.
- An interviewer can still ask "how would you split it?" The answer is in section 14.

**Why not a pile of microservices for a portfolio?** You would spend the interview drawing boxes. The control story is stronger if you can open one class and show that approval does not touch the limit.

---

## 3. Package map and the path from the button

```
com.creditsentinel
  CreditSentinelApplication
  config/          CreditProperties, LlmConfig, ToolConfig, DetectionConfig
  domain/          JPA entities and enums
  domain/math/     ExposureMath, CovenantMath, RatingWatch, PortfolioShare
  detection/       BreachDetector and the snapshot records it takes
  book/            BookReader and the view records tools return
  tools/           one Spring bean per tool, each with one @Tool method
  agent/           loop, scripted model, parser, citation check, drafting default
  service/         scan, investigate, decide
  web/             Thymeleaf desk and DeskQuery
  api/             JSON versions of the same actions
  repo/            Spring Data repositories
```

`BookReader` is the read model. Tools, the startup scan, and the desk all go through it, so "latest covenant reading" and "utilization" are not reimplemented in the UI.

Path for **Run investigation** on Northwind:

1. `POST /alerts/{alertId}/investigate` hits `DeskController.investigate`.
2. `InvestigationService.investigate` loads the alert. If a case is already `PENDING_APPROVAL`, `APPROVED`, or `REJECTED`, it returns that case id and does not call the model again.
3. Otherwise it creates a `CreditCase` with `CaseTrigger.BREACH`, deletes any previous memo and steps (a retry after `FAILED`), flushes, sets the case and the alert to `INVESTIGATING`, and calls `CreditReviewAgent.investigate`.
4. The agent puts `caseId` on the SLF4J MDC, builds auditing tool callbacks, and loops.
5. On a verified memo it saves `CreditMemo` and sets the case to `PENDING_APPROVAL`.
6. The controller redirects to `GET /cases/{id}`. `DeskQuery.casePage` reads steps, memo, and decision inside a transaction and returns a DTO. Templates never touch a lazy entity. `spring.jpa.open-in-view` is false, so a lazy read in the view would throw. That is intentional.

`InvestigationService` is the transaction boundary. The agent participates in that transaction. Tool reads see the seed data. Steps and the memo commit with the case. If the agent throws, it catches the exception, marks the case `FAILED`, sets the alert back to `OPEN`, and returns normally so the audit rows are not rolled back.

---

## 4. Schema, table by table

Flyway: `V1__schema.sql` creates tables, `V2__seed.sql` loads the book and the Pinnacle review. Hibernate `ddl-auto` is `validate`. The app will not start if an entity column disagrees with Postgres. Currency is `VARCHAR(3)`, not `CHAR(3)`, because Hibernate maps a `String` field of length 3 to varchar. A `CHAR` column failed validation when the test suite first booted.

Money columns are `NUMERIC(19,2)`. Ratios are `NUMERIC(19,4)`. Java uses `BigDecimal`. Utilization is not stored. It is divided when a snapshot or a tool response is built.

### borrower

`legal_name`, `sector` (enum name), `headquarters`, `relationship_start`. Eight rows. Sector is an enum, not free text, so `getSectorConcentration` cannot be called with a typo that silently becomes a new industry.

### facility

One row per borrower in the seed. `facility_type` is `REVOLVER` or `TERM_LOAN`. `committed_limit` and `outstanding` have no setters on the entity. There is no application path that updates them. That is the control, not an accident.

### covenant and covenant_reading

A covenant is the contractual test (`MIN_DSCR` or `MAX_LEVERAGE`). A reading is one test result: `threshold`, `actual`, `as_of_date`. Unique on `(facility_id, covenant_type)`. The reader uses the latest `as_of_date`. The seed has one reading each, dated 2026-10-31, so "latest" is unambiguous.

Term loans have DSCR only. Revolvers have DSCR and leverage. Harbor therefore cannot fail a leverage test it does not have.

### rating_event

Append-only history. Ordered by `effective_date`. The last row is the current rating. The row before it is the previous rating. One row means there is no previous rating and no crossing. The detector does not write rating events.

### credit_alert

Created by `BreachScanService` at startup, not by the seed. Columns: facility, `breach_type`, detail text, status (`OPEN`, `INVESTIGATING`, `CLOSED`). The scan skips a facility and breach type that already has a non-closed alert, so a restart does not duplicate Northwind.

### credit_case

The investigation. `alert_id` is nullable because the seeded Pinnacle review has no alert. `breach_type` is nullable for the same reason. `case_trigger` is `BREACH` or `PRIOR_REVIEW`. `failure_reason` is set when the agent gives up.

### agent_step

Append-only log of the loop. `step_index`, `step_kind` (`MODEL_TURN`, `TOOL_CALL`, `TOOL_RESULT`, `VERIFIER`), optional `tool_name`, payload text. The Pinnacle case has a memo and a decision and **no** steps. If you see a step, this loop wrote it during a run. That distinction is worth saying.

### credit_memo, cited_fact, memo_risk_driver, memo_missing_data

One memo per case. Recommendation and confidence are enums. Cited facts are child rows with an explicit `position` integer set in `CreditMemo.addFact`. An `@OrderColumn` was tried first. Hibernate inserted the fact and left `position` null, and the NOT NULL constraint failed. Setting the index in Java made the insert match the column. Risk drivers and missing-data lines are element collections ordered by `position`.

### human_decision

One row per case. Analyst id, `APPROVE` or `REJECT`, comment, timestamp. Unique on `case_id`.

---

## 5. Breach detection

`BreachDetector.detect` takes a `FacilitySnapshot` and returns a list of `Breach`. It does not load the database and it does not know what a model is. `BreachScanService` builds the snapshot through `BookReader` and saves an alert per breach.

Three rules, in this order:

1. **Limit.** `ExposureMath.overLimit`: outstanding `compareTo` committed limit `> 0`. The detail string uses `ExposureMath.money`, scale 2. At-the-limit is not a breach. Test: 25,000,000 versus 25,000,000 returns no breach. 27,000,000 versus 25,000,000 returns `LIMIT_UTILIZATION`.
2. **Covenant.** For each covenant, `CovenantMath.breached`. `MIN_DSCR` fails when actual `<` threshold. `MAX_LEVERAGE` fails when actual `>` threshold. All failures on one facility become **one** `COVENANT` breach whose detail joins the failing lines. A facility is not double-alerted for two bad covenants.
3. **Rating watch.** `RatingWatch.crossedBelowInvestmentGrade`. Previous and latest must both exist, the latest must be worse, the previous must be investment grade, and the latest must not be. `Rating.ordinal` increases as credit quality gets worse, so `isWorseThan` is `ordinal() > other.ordinal()`.

`BBB → BB` alerts. `A → BBB` does not. `BB → B` does not. `AA → A` does not. Missing ratings do not.

Startup on the seed therefore creates exactly two alerts. `CreditFlowTest` asserts `alerts.count() == 2`.

**Why the rules are not a prompt.** A model can miss Harbor or invent a third alert. A credit queue has to be repeatable. The unit tests run without a network and without Postgres.

---

## 6. The agent loop

Class: `CreditReviewAgent`. Inputs: the `ChatModel` bean, a `ToolCallingManager`, the list of `ToolCallback`s, `MemoParser`, `CitationVerifier`, repositories, a `MeterRegistry`, and `CreditProperties`.

`LlmConfig` registers `ScriptedCreditChatModel` as `@Primary` when `creditsentinel.llm-mode=scripted` (the default). The OpenAI starter still builds an `OpenAiChatModel` with a dummy key so the context starts offline. The primary bean is the one injected. In live mode the scripted bean is absent and the OpenAI model is the only `ChatModel`.

Each investigation:

1. Wraps every tool callback in `AuditingToolCallback`. The wrapper is created per run and closes over the case id and the evidence list. There is no thread-local.
2. Builds `DefaultToolCallingChatOptions` with those callbacks, `internalToolExecutionEnabled(false)`, and temperature 0. False means the chat model returns tool calls to us. We execute them. If this flag were true, the client would execute tools inside `chatModel.call` and the loop could not record the calls as its own steps.
3. Sends a system prompt (`InvestigationPrompt.system`) and a user prompt that contains only the breach type, borrower id, facility id, and sector. The whole book is not in the prompt. The model has to call tools.
4. Repeats up to `creditsentinel.max-model-turns` (6):
   - Call the chat model. Record a `MODEL_TURN` step. Time it as `agent.step`.
   - If the response has tool calls, `ToolCallingManager.executeToolCalls` runs each one. The auditing wrapper writes `TOOL_CALL` and `TOOL_RESULT` and keeps the JSON for the verifier. The conversation history returned by the manager, which now includes the tool responses, becomes the next prompt.
   - If the response is text, `MemoParser` strips an optional ``` fence and parses the JSON into `MemoDraft`. A blank summary, a missing enum, or a cited fact without name, value, and sourceTool is a parse failure.
   - `CitationVerifier` checks the draft against the evidence list.
   - Pass: save the memo, set `PENDING_APPROVAL`, return.
   - Fail, and this is the first failure: append `InvestigationPrompt.repair(...)` and continue. One repair only.
   - Fail again, or the turn budget is exhausted: set `FAILED`, copy the reason onto the case, set the alert back to `OPEN`.

`ScriptedCreditChatModel.call` is not a hidden shortcut around that loop. On the first call, when no `ToolResponseMessage` is present, it returns six parallel tool calls. The ids are `call_profile`, `call_exposure`, `call_covenant`, `call_rating`, `call_sector`, `call_prior`. Arguments are JSON taken from the user prompt with a line regex (`Borrower id:`, `Facility id:`, `Sector:`, `Breach type:`). On the second call it reads the tool-response JSON and builds the memo from those fields. Recommendation comes from `DraftingGuidance`, not from a sentence the fixture invented:

- limit breach → `REDUCE_LIMIT`
- rating watch → `ESCALATE`
- `MIN_DSCR` whose gap `(threshold - actual) / threshold` is **greater than** 0.10 → `ESCALATE`
- that gap equal to or under 0.10 → `WAIVE_WITH_CONDITIONS`
- any other covenant type, including `MAX_LEVERAGE` → `ESCALATE`

Harbor is `(1.25 - 1.05) / 1.25 = 0.16`, so ESCALATE. The summary text is assembled from `asText()` of the tool JSON, and the cited facts use those same strings. That is why the verifier passes in scripted mode.

A live model is instructed with the same drafting paragraph. Tests do not call it. Do not tell an interviewer the live model is proven. Tell them the loop is the same object and the fixture replaces only `ChatModel`.

---

## 7. The six tools

Each class is a `@Component` with one `@Tool` method. `ToolConfig` turns them into callbacks with `ToolCallbacks.from(...)`. Parameter names are the JSON keys (`borrowerId`, `facilityId`, `sector`). The compiler flag `parameters` is on, via the Spring Boot parent, so Spring AI can see those names.

All six are read-only. None of them accept a new limit, a new rating, or a waiver flag.

| Tool | Argument | Returns |
|---|---|---|
| `getBorrowerProfile` | borrower UUID | legal name, sector, headquarters, relationship start |
| `getFacilityExposure` | facility UUID | type, committed limit, outstanding, utilization to 4 decimals, `overLimit`, currency |
| `getCovenantStatus` | facility UUID | each covenant's type, threshold, actual, `breached`, direction `MINIMUM` or `MAXIMUM` |
| `getRatingHistory` | borrower UUID | events, latest, previous, `crossedBelowInvestmentGrade` |
| `getSectorConcentration` | sector enum name | sector outstanding, total outstanding, share, policy cap, `abovePolicyCap` |
| `searchPriorCases` | borrower UUID | approved or rejected cases only, so the in-progress case is excluded. Count plus summary and recommendation |

`BookReader` formats money with scale 2 and ratios with scale 4 before they become JSON strings. The memo cites those strings. Numeric comparison in the verifier also accepts `1.08` versus `1.0800`, because both parse as `BigDecimal` and `compareTo` is 0.

`getSectorConcentration` sums every facility. Northwind's 27 million is inside the 42 million industrials figure. That is correct: concentration includes the name you are reviewing.

Unknown ids throw `IllegalArgumentException` from `UUID.fromString` or from a missing row. `CreditReviewAgent` catches runtime exceptions and fails the case. The scripted model only passes ids that were in the prompt, which were loaded from the alert's facility.

---

## 8. Citation check

`CitationVerifier.verify` requires at least one cited fact. For each fact:

- Some tool result's name must equal `sourceTool`. Otherwise the error is "cites X, which was not called."
- The cited value must appear as a scalar in that tool's JSON. Objects and arrays are walked. If both the citation and a scalar parse as `BigDecimal`, equality is numeric. Otherwise the strings must match. `BBB` matches the rating scalar. `9.99` does not match a payload whose utilization is `1.0800`.

The unit test `CitationVerifierTest.rejectsAUtilizationFigureNoToolReturned` is that case.

What it does **not** check: that the number is the right field. A utilization citation that copied the committed limit would pass if that number was somewhere in `getFacilityExposure`. Say that before an interviewer does. The check is provenance, not semantics.

One repair turn sends the error text back and asks for JSON only. A second failure stops. There is no silent pass.

---

## 9. HTTP routes and pages

Thymeleaf:

| Method and path | What it does |
|---|---|
| `GET /` | Book, open alerts, human decisions |
| `POST /alerts/{alertId}/investigate` | Runs the agent, redirects to the case |
| `GET /cases/{caseId}` | Trace, memo, decision form if status is `PENDING_APPROVAL` |
| `POST /cases/{caseId}/decision` | Validates the form. On `IllegalStateException`, redirects back with a flash error |
| `GET /audit?caseId=` | Steps for one case, or every step if the parameter is omitted |

JSON, `CreditApiController`, same services:

| Method and path | Body |
|---|---|
| `GET /api/portfolio` | The desk DTO |
| `GET /api/alerts` | Open alerts |
| `POST /api/alerts/{alertId}/investigate` | `{ "caseId": "..." }` |
| `GET /api/cases/{caseId}` | Case page DTO |
| `POST /api/cases/{caseId}/decision` | `DecisionRequest` of analyst id, decision, comment |
| `GET /api/cases/{caseId}/audit` | Steps |

`ApiErrors` is bound to `CreditApiController` only. A missing case on the API is 404. A decision on a case that is not `PENDING_APPROVAL` is 409 with the sentence from `DecisionService`: "Only a case waiting for approval can be decided." Validation failures are 400. The HTML controller catches the same exceptions and redirects, because a JSON error page would be the wrong response for a form post.

`DecisionService` checks status before insert. Approving twice throws. The second call in `CreditFlowTest` is that guard. After a successful approve the test reads the facility again and compares the limit to `25000000.00` with `compareTo`, so scale differences do not look like a change.

There is no authentication. The analyst id is whatever the form submits. That is a demo weakness, listed below.

---

## 10. Audit rows and metrics

Every model turn, tool call, tool result, and verifier outcome is an `agent_step` row. The case page and `/audit` render the payload in a `<pre>`. Tool results are the JSON the verifier saw.

Micrometer timers:

- `agent.step`, tags `caseId` and `kind=MODEL_TURN`
- `tool.call`, tags `caseId` and `tool`

Actuator exposes `health`, `info`, and `metrics` only. Health details are hidden. `env` is not exposed, so a live API key in the environment is not on an HTTP endpoint.

**Why `caseId` as a metric tag is a demo choice.** A Prometheus label with unbounded values becomes a new time series per investigation. Fine for eight borrowers. In production the correlation id belongs on the log line, which is why the agent also puts `caseId` on the MDC, and the metric tag should be low-cardinality (`kind`, `tool`, `breachType`). Do not defend the tag as production practice. Defend it as a deliberate shortcut you can name.

---

## 11. Two end-to-end traces

### Northwind

1. Startup loads the revolver: limit 25000000.00, outstanding 27000000.00, rating BBB then BBB, DSCR 1.8000 versus 1.2500, leverage 2.4000 versus 3.5000.
2. `BreachDetector` emits one breach, `LIMIT_UTILIZATION`. The scan inserts an `OPEN` alert. No rating breach, no covenant breach.
3. The analyst posts the alert id. A case is created, trigger `BREACH`, status `INVESTIGATING`. The alert moves to `INVESTIGATING`.
4. Model turn 1 returns six tool calls. The manager executes them. Evidence includes utilization `1.0800`, `overLimit` true, industrials share `0.4200`, prior case count `0` for Northwind, latest rating `BBB`.
5. Model turn 2 returns JSON. `DraftingGuidance.forLimitBreach` is `REDUCE_LIMIT`. Cited facts copy tool strings, including utilization `1.0800`. The verifier passes.
6. Memo is stored. Case is `PENDING_APPROVAL`. The limit column has not been written.
7. The analyst approves. `human_decision` is inserted. Case is `APPROVED`. Alert is `CLOSED`. A second decision throws `IllegalStateException`. The limit is still 25000000.00.

### Harbor

1. Term loan, outstanding 9000000.00 against 15000000.00. DSCR actual 1.0500, minimum 1.2500. Rating BB then BB.
2. The limit rule does not fire. The rating rule does not fire. The covenant rule fires.
3. The same six tools run. The breached covenant line is `MIN_DSCR`.
4. Gap 0.1600 is greater than 0.10, so the recommendation is `ESCALATE`.
5. The memo cites outstanding, limit, utilization, rating, sector share, cap, prior count, `dscrActual` 1.0500, and `dscrThreshold` 1.2500. The verifier checks those strings against tool JSON.
6. The case waits. Rejecting it records `REJECTED` and closes the alert. The covenant reading stays 1.0500.

### Pinnacle, which the loop does not produce

Seeded case, trigger `PRIOR_REVIEW`, status `APPROVED`, recommendation `WATCH`, decision by `m.chen`. No `agent_step` rows. `searchPriorCases` returns it when the borrower is Pinnacle. It returns a count of 0 for Northwind. The case page says the absence of steps means the review was seeded.

---

## 12. Design choices and the alternative that was rejected

**Spring AI `ChatModel` plus our loop, instead of a hand-rolled HTTP client.** The tool-call protocol, the JSON schema for tools, and the live provider stay in the library. The stop conditions, the repair limit, and the audit steps stay in `CreditReviewAgent`, where an interviewer can read them. A raw HTTP client would have meant reimplementing function-call parsing for no extra control.

**`internalToolExecutionEnabled(false)`, instead of letting the client run tools inside `call`.** Internal execution hides the round trip. We need a row per call. The manager is still used, so we do not reimplement argument matching.

**Thymeleaf, instead of a separate React app.** The interview claim is Java. A second toolchain would dominate the repo and add a build that can fail independently of the control story. The API is there if the UI is dismissed as incidental.

**Scripted `ChatModel`, instead of mocking the whole agent.** A mock that returns a memo never proves the tools ran or that the verifier saw their JSON. The fixture speaks tool calls. `CreditFlowTest` fails if utilization `1.0800` is not cited, which only happens if exposure was actually read.

**Rules in Java, instead of asking the model to find breaches.** Covered in section 5. The model is not on the detection path even in live mode. The scan runs before any chat client is called.

**DTOs in `DeskQuery`, instead of passing entities to Thymeleaf.** Open-session-in-view would have hidden lazy loads and kept a database connection for the whole render. It is off. The test bug that showed up while building this was exactly a lazy `citedFacts` read after the session closed. The fix was to read through `DeskQuery`, which is `@Transactional(readOnly = true)` and copies facts into `FactRow` before it returns.

**Explicit `position` on cited facts, instead of `@OrderColumn`.** Hibernate's insert omitted the order column and Postgres rejected the null. Owning the integer in `addFact` is more code and it is obvious.

---

## 13. Known weaknesses

Say these without being asked if the interviewer is looking for judgment.

- One process, one Postgres. No horizontal scale, no outbox, no second availability zone.
- No authentication. Anyone who can open the port can approve a memo. The analyst id is a form field.
- Approving closes the alert and does not re-run detection. A still-broken covenant disappears from the open queue until the data changes and a new non-closed alert is allowed. The scan will not recreate an alert while a closed one is the latest row it looks at: it only skips non-closed alerts, so a **restart would open a new alert** for the same breach. That restart behavior is worth knowing. The in-memory demo hides it if you do not restart.
- The citation check does not bind a value to a field name. It binds it to "this tool returned this scalar."
- Scripted recommendations are Java (`DraftingGuidance`). A live model gets the same instructions and can ignore them. There is no test that a live model obeys the 10% line.
- `caseId` on a Micrometer timer is high cardinality.
- The OpenAI client is constructed even in scripted mode, with a dummy key, so a mis-set primary bean would call the network. The primary annotation is the guard.
- No idempotency key on the decision beyond the status check. Two concurrent approves could both pass the status read. The demo is single-threaded. A unique constraint on `human_decision.case_id` would turn the second insert into a database error, which is the real lock. The status check is the friendly error.

---

## 14. Likely interview questions

**Q: Where is the agent, and where is it not?**
A: `CreditReviewAgent` is the loop: model turn, tool execution, parse, citation check, one repair, then stop. `BreachDetector` is not an agent. `DecisionService` is not an agent. The model never sees a method that writes a limit, because that method does not exist. §5, §6, §9.

**Q: Why not let the model detect the breaches?**
A: The queue has to be the same on every restart and in a unit test with no API key. The model is allowed to explain a breach the rules already found. §5.

**Q: What happens if the model invents a utilization?**
A: `CitationVerifier` walks the tool JSON. A number that does not match any scalar fails the draft. One repair turn is allowed. The second failure sets the case to `FAILED` and reopens the alert. §8.

**Q: Why is the scripted model still an agent?**
A: It returns tool calls. The production loop executes the real Spring tools against Postgres and refuses a memo that does not cite them. Swapping in a live `ChatModel` does not change `InvestigationService` or the tools. What is scripted is the choice of recommendation, via `DraftingGuidance`, so the demo cannot drift. §6.

**Q: Show me that approval does not change the limit.**
A: `Facility` has getters only. `DecisionService.decide` writes a `HumanDecision`, updates case status, and closes the alert. `CreditFlowTest` approves Northwind and asserts the committed limit still compares equal to 25000000.00. §4, §9, §11.

**Q: Why one Spring Boot process?**
A: Different scaling would matter if detection, the model, and the API had different lifecycles. Here the thing to explain is the control boundary, and a single transaction keeps the audit rows with the case. §2.

**Q: How would you scale the agent?**
A: Put investigations on a queue, run the agent workers separately, and keep Postgres as the source of truth for alerts, steps, and decisions. Do not put `caseId` on a metric. Use the MDC correlation id on logs and a trace. The tools stay read-only. The decision API stays the only writer of `human_decision`. §10, §13.

**Q: Why `BigDecimal`?**
A: Money and ratios. `double` cannot be trusted for a comparison that decides an alert. Utilization uses scale 4, half up. The breach comparison is `compareTo`, not subtraction into a float. §4, §5.

**Q: What would you not automate?**
A: Setting a new limit, assigning a rating, and granting a waiver. Those are authority. The memo can recommend them. The human decision records agreement with the draft and still does not perform them. `docs/FINANCE_GUIDE.md` §7 and §10.

**Q: Biggest hole?**
A: No auth, the citation check is provenance rather than field identity, and a process restart can open a fresh alert for a breach whose previous alert was closed. All three are known. §13.
