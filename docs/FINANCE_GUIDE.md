# Credit Sentinel — finance study guide

Read this before an interview. Every number below is the number in the seed data and the number the running app uses. If a threshold changes in code, this file has to change with it.

You do not need a markets background. You need to be able to say, in plain words, what the screen is showing and what the app is **not** deciding.

---

## 1. What this app is, in one paragraph

Credit Sentinel is a queue for a junior credit analyst. Eight borrowers share a made-up loan book. Java compares each facility with rules that are already written down. When a rule fails, an alert appears. An agent may then look up facts with tools and draft an internal note, called a credit memo, that recommends a next step. A human records approve or reject. The app never grants credit, never assigns a new rating, and never changes a committed limit.

Say this if you only get ten seconds: "Detection is deterministic Java. The model drafts from tool results. Authority stays with the analyst."

---

## 2. The people and the loans

### Borrower

A borrower is the company that owes the bank money. In this app each borrower has a legal name, a sector, a headquarters, and the date the relationship started. Northwind Manufacturing is a borrower. Its sector is INDUSTRIALS.

### Facility

A facility is one loan, or one credit line, with that borrower. The book gives every borrower exactly one facility so the demo stays easy to narrate. A real borrower can have several. The code allows that: tools take a facility id, not "the borrower's only loan."

Two kinds appear in the seed:

| Kind | What it means here | Who has one |
|---|---|---|
| REVOLVER | A revolving line. The bank commits a maximum. The borrower draws, repays, and can draw again, up to that maximum. | Northwind, Cedar Health, BrightGrid Energy, Metro Transit Leasing, Apex Packaging, Luna Software |
| TERM_LOAN | A loan with an amount outstanding that is paid down. It still has a committed amount and an outstanding amount, so the same utilization math applies. | Harbor Retail, Pinnacle Logistics |

### Committed limit, outstanding, utilization

- **Committed limit**: the maximum the bank has agreed the borrower may draw on that facility. It is a number already stored on the facility. This app does not choose it.
- **Outstanding**: how much is currently drawn. Also stored. This app does not choose it.
- **Utilization** = outstanding ÷ committed limit, rounded to 4 decimal places. `ExposureMath.utilization` is the only place that division happens.

Equal to the limit is **not** a breach. The rule is strict: outstanding **greater than** committed limit. `ExposureMath.overLimit` uses `compareTo > 0`.

Worked example, Northwind Manufacturing:

- Committed limit 25,000,000.00
- Outstanding 27,000,000.00
- Utilization 27,000,000 ÷ 25,000,000 = 1.0800
- On the book page that is shown as 108.00%
- The alert text is: "Outstanding 27000000.00 exceeds committed limit 25000000.00"

They have drawn 2,000,000 more than the bank committed. That is the whole limit breach. It is not a judgment about whether Northwind is a good company.

Harbor is the contrast. Committed 15,000,000, outstanding 9,000,000, utilization 0.6000 (60%). Harbor is **not** over its limit. Harbor's alert is a covenant, described below.

---

## 3. How a rating works in this app

### The scale

The app uses an internal scale, strongest first:

`AAA, AA, A, BBB, BB, B, CCC`

Investment grade means AAA, AA, A, or BBB. Non-investment grade means BB, B, or CCC. The line sits between BBB and BB. `Rating.isInvestmentGrade()` is `ordinal() <= BBB`.

This is a simplified scale for the demo. It is not a live S&P rating, not a Moody's rating, and not a probability of default. Say that out loud if someone asks "is this their real rating?"

### The app does not decide the rating

Ratings are rows in `rating_event`, loaded by Flyway. Each row is a rating and the date it became effective. The latest row is the current rating. The one before it is the previous rating.

`BreachDetector` does not pick a new rating. It asks one yes-or-no question, implemented in `RatingWatch.crossedBelowInvestmentGrade`:

A watch alert fires only when **all** of these are true:

1. There is a previous rating and a latest rating.
2. The latest rating is worse than the previous one (a downgrade, not an upgrade).
3. The previous rating was investment grade (BBB or stronger).
4. The latest rating is not investment grade (BB or weaker).

So the only crossing that matters is "fell out of investment grade."

| Borrower | Previous | Latest | Alert? | Why |
|---|---|---|---|---|
| Northwind | BBB | BBB | No | No change |
| Harbor | BB | BB | No | Already non-investment grade, and it did not move |
| Luna Software | AA | A | No | Downgrade, but both sides are still investment grade |
| Pinnacle Logistics | A on 2024-06-01 | BBB on 2026-02-01 | No | Downgrade of one notch, still investment grade |
| Apex Packaging | BB | BB | No | Stays non-investment grade. BB to B would also **not** alert, because the previous rating was already outside investment grade |
| A case that exists only in the unit test | BBB | BB | Yes | This is the crossing the rule is written for. The seed book has no live example, on purpose |

Pinnacle is the teaching case for "a downgrade that is not an alert." There is a seeded annual review, not an alert. The memo says the rating moved from A to BBB, recommends WATCH, and the analyst `m.chen` approved it on 2026-03-20 with the comment that the limit stays unchanged. Open that case from Human decisions on the book page.

If an interviewer asks "how do you decide the rating?": "I don't. The rating is an input. The only rating decision in the code is whether a downgrade crossed from BBB or better to BB or worse. That creates an alert. It does not write a new rating."

---

## 4. Covenants

A covenant is a condition in the loan agreement. The borrower promises to stay on the right side of a number. The bank tests that promise on a date. In the seed, every reading is as of 2026-10-31.

This app has two covenant types. `CovenantMath.breached` is the rule:

| Type | Plain meaning | Fails when | Equal to the line |
|---|---|---|---|
| MIN_DSCR | Debt service coverage. Cash flow relative to debt payments. Higher is safer. | actual **<** minimum | Passes. The borrower is exactly on the line, not past it. |
| MAX_LEVERAGE | Debt relative to earnings. Higher is riskier. | actual **>** maximum | Passes. |

Every facility has a MIN_DSCR covenant. Revolvers also have a MAX_LEVERAGE covenant. Harbor and Pinnacle are term loans, so they only have DSCR.

You do not need the accounting definition of "cash flow" to explain the demo. You need the direction: for DSCR, bigger is safer, and the contract names a floor.

### Harbor, the live covenant breach

Harbor Retail, term loan:

- Minimum DSCR 1.2500
- Actual DSCR 1.0500
- 1.0500 is below 1.2500, so the covenant fails
- Utilization is 60%, so this is not a limit breach
- Rating is BB and was already BB, so this is not a rating-watch breach

The alert is type COVENANT. The detail mentions MIN_DSCR.

How far through the floor:

gap = (threshold − actual) ÷ threshold = (1.25 − 1.05) ÷ 1.25 = 0.1600

That is 16% below the minimum. The drafting rule treats a gap **greater than** 10% as material. Harbor is material. The scripted draft therefore recommends ESCALATE, not a waiver.

A smaller miss is different. If actual were 1.1250 against a minimum of 1.2500, the gap would be exactly 0.1000. The comparison is `gap > 0.10`, so exactly 10% is drafted as WAIVE_WITH_CONDITIONS. That case is a unit test, not a borrower on the book. Say that. Do not pretend Harbor is a mild miss.

Northwind's covenants pass, so you can say the limit problem is not also a covenant problem:

- DSCR actual 1.8000 versus minimum 1.2500
- Leverage actual 2.4000 versus maximum 3.5000

### What a waiver is

A waiver means the bank agrees, for a period, not to treat a broken covenant as a default. It is a credit decision. It often comes with conditions: a cure deadline, extra reporting, a fee. `WAIVE_WITH_CONDITIONS` is a **recommendation the draft may make**. Recording APPROVE on that draft records that the analyst agrees with the note. It does not edit the covenant and it does not mark the covenant as passed.

---

## 5. Sector concentration

Concentration is about the **book**, not one borrower. It asks: of all money currently drawn, how much sits in one industry?

Share = sector outstanding ÷ total outstanding, 4 decimal places. The policy cap in `application.yml` is 0.35 (35%). Above the cap means share **greater than** 0.35. Equal would not be above.

The seed outstanding amounts sum to 100,000,000.00:

| Borrower | Sector | Outstanding |
|---|---|---|
| Northwind Manufacturing | INDUSTRIALS | 27,000,000 |
| Harbor Retail | CONSUMER | 9,000,000 |
| Cedar Health | HEALTHCARE | 18,000,000 |
| BrightGrid Energy | ENERGY | 16,000,000 |
| Metro Transit Leasing | INDUSTRIALS | 6,000,000 |
| Apex Packaging | INDUSTRIALS | 5,000,000 |
| Luna Software | TECHNOLOGY | 15,000,000 |
| Pinnacle Logistics | INDUSTRIALS | 4,000,000 |

Industrials = 27 + 6 + 5 + 4 = 42,000,000. Share = 0.4200. Cap = 0.3500. The tool returns `abovePolicyCap: true`.

There is **no** concentration alert. The detector does not create one. The agent is supposed to read `getSectorConcentration` and mention the cap in the memo as a risk driver. That is a portfolio fact sitting next to a single-name problem: Northwind is over its own limit, and the book is also heavy in industrials.

---

## 6. What a credit memo is

A credit memo is an internal note for a credit officer. It states the facts, why they matter, and a recommended next step. It is not a contract, not a loan approval, and not a rating action.

In this app the memo has:

- **summary** — a few sentences
- **cited facts** — each one is a name, a value, and the tool that returned that value
- **risk drivers** — short reasons
- **missing data** — something the tools cannot see. The scripted draft always says no borrower explanation is on file
- **recommendation** — one of the four enums below
- **confidence** — LOW, MEDIUM, or HIGH. The scripted draft uses MEDIUM because the explanation letter is missing

The citation check refuses the memo if a cited value never came back from the named tool. That is how the app stops the draft from inventing a utilization figure.

---

## 7. The four recommendations

These are drafting labels. They are not bookings.

| Recommendation | What you say it means | Who is allowed to act |
|---|---|---|
| WATCH | Keep observing. Do not change the limit. Pinnacle's annual review is the example: A to BBB, still investment grade. | The human. The seeded decision is APPROVE by m.chen, and the limit stayed 8,000,000. |
| REDUCE_LIMIT | Ask a credit officer to reduce the commitment so outstanding is no longer above it. Northwind's draft says this. The draft does **not** contain a new limit number. | The human, later, outside this app. Approving the memo here does not write a new committed limit. |
| WAIVE_WITH_CONDITIONS | Ask a human to tolerate a covenant miss for a while if the borrower does something specific. Used by the drafting rule only when a DSCR miss is 10% of the minimum or less. | The human. This app has no "grant waiver" button. |
| ESCALATE | This desk should not decide it. Harbor's DSCR miss is the live example. A rating that crosses out of investment grade would also escalate. A leverage breach escalates. | Someone more senior. The analyst can still approve or reject the **draft**. |

`DraftingGuidance` is the Java version of that default, used by the scripted narrator. The same sentences are in the prompt for a live model. A live model can ignore them. The human decision is the outcome either way.

Say this if asked "is 10% a real bank policy?": "No. It is a drafting default I wrote so the recommendation is explainable and testable. A credit officer uses judgment. They can reject the draft, and the app stores that disagreement."

---

## 8. What the human decision means

The form asks for an analyst id, APPROVE or REJECT, and a comment. `DecisionService.decide` will only do this when the case status is `PENDING_APPROVAL`. It then:

- writes a `human_decision` row
- sets the case to APPROVED or REJECTED
- closes the alert

It does not call any method that sets `committedLimit`. The facility entity has no setter for the limit. After you approve Northwind, the book still shows 25000000.00.

REJECT means "I do not accept this draft." It does not mean "the breach went away." The alert is closed so this same alert is not investigated again. If you restart the app, the scan looks only for a non-closed alert of that type. A closed one does not count, so startup inserts a new open alert for the same breach. That is called out in the backend deep dive as a weakness.

---

## 9. Spoken walkthroughs

### Northwind

"Northwind Manufacturing has a revolving credit line. The bank committed 25 million. They have drawn 27 million, so utilization is 1.08, above the commitment. Their internal rating is BBB and it did not change, so this is not a rating event. DSCR is 1.80 against a minimum of 1.25, so the covenant passes. The draft recommends REDUCE_LIMIT. That means a person should consider cutting the commitment. The memo does not choose the new number, and when I approve it the stored limit is still 25 million. Industrials are 42% of outstanding against a 35% cap, so the memo also flags portfolio concentration."

### Harbor

"Harbor Retail has a term loan, 15 million committed, 9 million outstanding, so they are inside the limit. The problem is the DSCR covenant. The contract floor is 1.25. The latest actual is 1.05. The miss is 16% of the floor, which is past the 10% drafting line, so the draft says ESCALATE rather than waive. I can reject that recommendation. Rejecting it records my decision. It does not waive the covenant."

### Pinnacle, if they ask about ratings

"Pinnacle was downgraded from A to BBB on 1 February 2026. BBB is still investment grade, so the detector does not open a watch alert. An earlier annual review recommended WATCH. Analyst m.chen approved that and left the 8 million limit unchanged."

---

## 10. Real bank versus this demo

Name these so you do not overclaim. Then say they are outside this app.

- **Probability of default (PD)**: the chance the borrower fails to pay. This app does not estimate it.
- **Loss given default (LGD)**: how much of the exposure the bank would lose if the borrower failed. Not estimated here.
- **Exposure at default**: how much would be outstanding at the moment of default. Not estimated here.
- **Regulatory capital / risk-weighted assets**: how much capital a regulator says the bank must hold against the loan. Not calculated here.
- **IFRS 9 / CECL provisioning**: accounting for expected credit loss. Not in this app.
- **Internal rating model**: a real bank's rating is the output of a governed model, with overrides and a committee. Here the rating is seed data plus one crossing rule.
- **Credit committee**: a group with authority to cut a limit or grant a waiver. This app stops at the analyst's approve/reject of a draft.

The honest sentence: "I separated detection, drafting, and authority because that is the control a bank cares about. I did not rebuild a capital calculator."

---

## 11. Glossary

- **Borrower** — the company.
- **Facility** — one revolver or term loan.
- **Revolver** — a line that can be redrawn up to the commitment.
- **Committed limit** — the agreed maximum.
- **Outstanding** — the amount currently drawn.
- **Utilization** — outstanding divided by the committed limit.
- **Investment grade** — AAA through BBB on this app's scale.
- **Covenant** — a contractual test.
- **DSCR** — debt service coverage; higher is safer; this app fails it when actual is below the minimum.
- **Leverage** — debt versus earnings; higher is riskier; this app fails it when actual is above the maximum.
- **Waiver** — an agreement to tolerate a breach for a time. This app can only recommend one.
- **Concentration** — one sector's share of total outstanding.
- **Credit memo** — the internal draft note.
- **Watch** — keep observing, no limit change.
- **Escalate** — send the case off this desk.

---

## 12. Questions and the answer to give

**What is a credit memo?**
An internal note that lists the facts, the risk, and a recommended next step for a credit officer. In this app it is a draft. It becomes an outcome only when a human approves or rejects it. It is not the loan agreement.

**How do you decide the rating?**
The app does not decide it. Ratings are stored events. The detector only checks whether a downgrade crossed from investment grade, BBB or stronger, to non-investment grade, BB or weaker. Luna going from AA to A does not alert. BBB to BB would.

**How do you decide the limit?**
The app does not set a limit. It compares outstanding with the committed limit already on the facility. If outstanding is greater, the alert is a limit breach and the draft recommends REDUCE_LIMIT, without a replacement number. Approving the memo leaves the committed limit column unchanged. I can show Northwind before and after: 25000000.00 both times.

**What happens when DSCR breaches?**
The latest actual is compared with the minimum. Harbor is 1.05 against 1.25, so an alert is opened. The drafting rule escalates if the shortfall is more than 10% of the minimum, which Harbor is. A miss of 10% or less would be drafted as a waiver with conditions. Neither path changes the covenant. The analyst decides.

**Why can the model not just approve the case?**
Because drafting and authority are different. The model can be wrong, and a limit change or a waiver is a credit decision. The status stops at PENDING_APPROVAL. `DecisionService` rejects a decision on any other status.

**What is the 42% number?**
Industrials outstanding divided by the whole book. 42 million over 100 million, against a 35% cap. It is context for the memo, not a third alert.
