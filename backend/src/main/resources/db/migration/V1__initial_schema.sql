-- Initial schema matching JPA entities (snake_case physical naming).

CREATE TABLE clients (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(32)  NOT NULL UNIQUE,
    name        VARCHAR(128) NOT NULL
);

CREATE TABLE patients (
    id             BIGSERIAL PRIMARY KEY,
    mrn            VARCHAR(32) NOT NULL UNIQUE,
    first_name     VARCHAR(64) NOT NULL,
    last_name      VARCHAR(64) NOT NULL,
    date_of_birth  DATE        NOT NULL,
    sex            VARCHAR(16)
);

CREATE TABLE test_catalog (
    id         BIGSERIAL PRIMARY KEY,
    code       VARCHAR(32)  NOT NULL UNIQUE,
    name       VARCHAR(128) NOT NULL,
    tube_type  VARCHAR(64)  NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE users (
    id             BIGSERIAL PRIMARY KEY,
    username       VARCHAR(64)  NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    role           VARCHAR(32)  NOT NULL,
    enabled        BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE orders (
    id            BIGSERIAL PRIMARY KEY,
    order_number  VARCHAR(32) NOT NULL UNIQUE,
    client_id     BIGINT      NOT NULL REFERENCES clients (id),
    patient_id    BIGINT      NOT NULL REFERENCES patients (id),
    created_at    TIMESTAMPTZ NOT NULL,
    created_by    VARCHAR(64) NOT NULL
);

CREATE TABLE order_tests (
    id        BIGSERIAL PRIMARY KEY,
    order_id  BIGINT NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    test_id   BIGINT NOT NULL REFERENCES test_catalog (id)
);

CREATE TABLE specimens (
    id             BIGSERIAL PRIMARY KEY,
    order_id       BIGINT       NOT NULL REFERENCES orders (id),
    barcode        VARCHAR(32)  NOT NULL UNIQUE,
    tube_type      VARCHAR(64)  NOT NULL,
    collected_at   TIMESTAMPTZ,
    received_at    TIMESTAMPTZ,
    status         VARCHAR(32)  NOT NULL,
    status_reason  VARCHAR(500),
    created_at     TIMESTAMPTZ  NOT NULL
);

CREATE TABLE specimen_results (
    id            BIGSERIAL PRIMARY KEY,
    specimen_id   BIGINT       NOT NULL REFERENCES specimens (id) ON DELETE CASCADE,
    analyte       VARCHAR(64)  NOT NULL,
    result_value  VARCHAR(128) NOT NULL,
    unit          VARCHAR(32),
    flagged       BOOLEAN      NOT NULL,
    resulted_at   TIMESTAMPTZ  NOT NULL,
    entered_by    VARCHAR(64)  NOT NULL
);

CREATE TABLE audit_log (
    id            BIGSERIAL PRIMARY KEY,
    entity_type   VARCHAR(32)  NOT NULL,
    entity_id     BIGINT       NOT NULL,
    action        VARCHAR(48)  NOT NULL,
    details       VARCHAR(1000),
    performed_by  VARCHAR(64)  NOT NULL,
    performed_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_audit_entity ON audit_log (entity_type, entity_id);
