CREATE TABLE borrower (
    id UUID PRIMARY KEY,
    legal_name VARCHAR(255) NOT NULL,
    sector VARCHAR(255) NOT NULL,
    headquarters VARCHAR(255) NOT NULL,
    relationship_start DATE NOT NULL
);

CREATE TABLE facility (
    id UUID PRIMARY KEY,
    borrower_id UUID NOT NULL REFERENCES borrower (id),
    facility_type VARCHAR(255) NOT NULL,
    committed_limit NUMERIC(19, 2) NOT NULL,
    outstanding NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL
);

CREATE TABLE covenant (
    id UUID PRIMARY KEY,
    facility_id UUID NOT NULL REFERENCES facility (id),
    covenant_type VARCHAR(255) NOT NULL,
    CONSTRAINT uq_covenant_facility_type UNIQUE (facility_id, covenant_type)
);

CREATE TABLE covenant_reading (
    id UUID PRIMARY KEY,
    covenant_id UUID NOT NULL REFERENCES covenant (id),
    threshold NUMERIC(19, 4) NOT NULL,
    actual NUMERIC(19, 4) NOT NULL,
    as_of_date DATE NOT NULL
);

CREATE TABLE rating_event (
    id UUID PRIMARY KEY,
    borrower_id UUID NOT NULL REFERENCES borrower (id),
    rating VARCHAR(255) NOT NULL,
    effective_date DATE NOT NULL
);

CREATE TABLE credit_alert (
    id UUID PRIMARY KEY,
    facility_id UUID NOT NULL REFERENCES facility (id),
    breach_type VARCHAR(255) NOT NULL,
    detail TEXT NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE credit_case (
    id UUID PRIMARY KEY,
    alert_id UUID REFERENCES credit_alert (id),
    borrower_id UUID NOT NULL REFERENCES borrower (id),
    facility_id UUID NOT NULL REFERENCES facility (id),
    breach_type VARCHAR(255),
    case_trigger VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    failure_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE agent_step (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL REFERENCES credit_case (id),
    step_index INT NOT NULL,
    step_kind VARCHAR(255) NOT NULL,
    tool_name VARCHAR(255),
    payload TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE credit_memo (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL UNIQUE REFERENCES credit_case (id),
    summary TEXT NOT NULL,
    recommendation VARCHAR(255) NOT NULL,
    confidence VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE memo_risk_driver (
    memo_id UUID NOT NULL REFERENCES credit_memo (id),
    position INT NOT NULL,
    driver TEXT NOT NULL,
    PRIMARY KEY (memo_id, position)
);

CREATE TABLE memo_missing_data (
    memo_id UUID NOT NULL REFERENCES credit_memo (id),
    position INT NOT NULL,
    item TEXT NOT NULL,
    PRIMARY KEY (memo_id, position)
);

CREATE TABLE cited_fact (
    id UUID PRIMARY KEY,
    memo_id UUID NOT NULL REFERENCES credit_memo (id),
    position INT NOT NULL,
    fact_name VARCHAR(255) NOT NULL,
    fact_value VARCHAR(255) NOT NULL,
    source_tool VARCHAR(255) NOT NULL
);

CREATE TABLE human_decision (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL UNIQUE REFERENCES credit_case (id),
    analyst_id VARCHAR(255) NOT NULL,
    decision VARCHAR(255) NOT NULL,
    comment TEXT NOT NULL,
    decided_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_facility_borrower ON facility (borrower_id);
CREATE INDEX ix_alert_facility ON credit_alert (facility_id, breach_type);
CREATE INDEX ix_case_borrower ON credit_case (borrower_id);
CREATE INDEX ix_step_case ON agent_step (case_id, step_index);
