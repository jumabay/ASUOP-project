-- H2 schema for automated local tests only. Not proof of Oracle compatibility.

CREATE TABLE rwl_import_run (
    run_id            BIGINT         NOT NULL PRIMARY KEY,
    started_at_utc    TIMESTAMP      NOT NULL,
    finished_at_utc   TIMESTAMP,
    mode_code         VARCHAR(16)    NOT NULL,
    status_code       VARCHAR(32)    NOT NULL,
    request_summary   VARCHAR(1000),
    error_summary     VARCHAR(2000),
    wagons_seen       INT            DEFAULT 0 NOT NULL,
    ops_seen          INT            DEFAULT 0 NOT NULL,
    inserted_cnt      INT            DEFAULT 0 NOT NULL,
    updated_cnt       INT            DEFAULT 0 NOT NULL,
    unchanged_cnt     INT            DEFAULT 0 NOT NULL,
    rejected_cnt      INT            DEFAULT 0 NOT NULL
);

CREATE SEQUENCE rwl_import_run_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE rwl_staging_operation (
    operation_id         BIGINT        NOT NULL PRIMARY KEY,
    run_id               BIGINT        NOT NULL,
    event_key            VARCHAR(512)  NOT NULL,
    content_hash         VARCHAR(64)   NOT NULL,
    revision_no          INT           NOT NULL,
    wagon_no             VARCHAR(32)   NOT NULL,
    station_code         VARCHAR(16),
    station_name         VARCHAR(256),
    operation_time_utc   TIMESTAMP     NOT NULL,
    operation_code       VARCHAR(64),
    operation_asoup_code VARCHAR(16),
    operation_name       VARCHAR(256),
    train_index          VARCHAR(64),
    train_number         VARCHAR(64),
    from_station_code    VARCHAR(16),
    from_station_name    VARCHAR(256),
    to_station_code      VARCHAR(16),
    to_station_name      VARCHAR(256),
    loaded_flag          VARCHAR(8),
    cargo_name           VARCHAR(512),
    etsng_code           VARCHAR(32),
    weight_tons          VARCHAR(32),
    latitude             VARCHAR(64),
    longitude            VARCHAR(64),
    source_code          VARCHAR(16)   DEFAULT 'RWL' NOT NULL,
    fetched_at_utc       TIMESTAMP     NOT NULL,
    created_at_utc       TIMESTAMP     NOT NULL,
    CONSTRAINT fk_rwl_op_run FOREIGN KEY (run_id) REFERENCES rwl_import_run (run_id),
    CONSTRAINT uq_rwl_op_event_rev UNIQUE (event_key, revision_no)
);

CREATE SEQUENCE rwl_staging_operation_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE rwl_staging_current (
    event_key      VARCHAR(512) NOT NULL PRIMARY KEY,
    operation_id   BIGINT       NOT NULL,
    content_hash   VARCHAR(64)  NOT NULL,
    updated_at_utc TIMESTAMP    NOT NULL,
    CONSTRAINT fk_rwl_cur_op FOREIGN KEY (operation_id) REFERENCES rwl_staging_operation (operation_id)
);

CREATE TABLE rwl_staging_reject (
    reject_id        BIGINT        NOT NULL PRIMARY KEY,
    run_id           BIGINT        NOT NULL,
    wagon_no         VARCHAR(32),
    reason_code      VARCHAR(64)   NOT NULL,
    detail_text      VARCHAR(2000),
    raw_excerpt      VARCHAR(2000),
    fetched_at_utc   TIMESTAMP     NOT NULL,
    created_at_utc   TIMESTAMP     NOT NULL,
    CONSTRAINT fk_rwl_rej_run FOREIGN KEY (run_id) REFERENCES rwl_import_run (run_id)
);

CREATE SEQUENCE rwl_staging_reject_seq START WITH 1 INCREMENT BY 1;
