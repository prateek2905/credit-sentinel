# Credit Sentinel

A Java credit-monitoring desk. Rules detect breaches. A tool-using agent drafts a memo. A person approves or rejects it. Nothing in that flow changes a committed limit.

Study these before an interview:

- [docs/FINANCE_GUIDE.md](docs/FINANCE_GUIDE.md) — the credit ideas, with the seed-book numbers
- [docs/BACKEND_DEEP_DIVE.md](docs/BACKEND_DEEP_DIVE.md) — what each class does and why it is shaped that way

## Run

Java 21 is required. This machine uses the Homebrew JDK, which is not on the default PATH:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export PATH="$JAVA_HOME/bin:$PATH"
```

Start Postgres, then the app. The database is published on port **5433**.

```bash
docker compose up -d
mvn spring-boot:run
```

Open http://localhost:8080. Two alerts are already open: Northwind Manufacturing (limit) and Harbor Retail (covenant). Use **Run investigation**, read the tool trace and the memo, then record a decision.

The default `creditsentinel.llm-mode` is `scripted`. The agent loop, the Java tools, and the citation check are real. Only the chat model is a fixture, so the demo does not need an API key. For a live model:

```bash
export LLM_MODE=live
export OPENAI_API_KEY=...
# optional: OPENAI_BASE_URL, OPENAI_MODEL
mvn spring-boot:run -Dspring-boot.run.arguments=--creditsentinel.llm-mode=live
```

`LLM_MODE` is not read automatically. Pass `--creditsentinel.llm-mode=live` as above, or change `application.yml`.

## Test

```bash
mvn test
```

`CreditFlowTest` starts Postgres with Testcontainers. On Docker Desktop the socket is `~/.docker/run/docker.sock`. The `docker-desktop` Maven profile points Surefire at that socket when the file exists. Start Docker before `mvn test`.

## Ten-minute demo

1. Show the book. Northwind utilization is 108%. Harbor's DSCR is 1.05 against a minimum of 1.25. Say the detector, not the model, created those two alerts.
2. Open the Pinnacle Logistics decision. Rating moved A to BBB, which is still investment grade, so there is no rating alert. The recorded recommendation is WATCH, and the limit was not changed.
3. Run Northwind. Walk the six tool calls, then the memo. Recommendation is REDUCE_LIMIT. Point at the committed limit and say approval will not write a new number.
4. Approve it with a comment. Refresh. The limit is still 25000000.00. The audit page separates the model draft from the human decision.
5. Run Harbor. The DSCR miss is more than 10% of the minimum, so the draft says ESCALATE. Reject it if you want to show that the human can disagree.
