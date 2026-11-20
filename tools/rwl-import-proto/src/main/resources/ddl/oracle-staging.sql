-- RWL import prototype staging for new Oracle RAILDB.
-- Creates objects in the connected user's schema (from RAILDB_* / ORACLE_* config).
-- Does not touch legacy RAC/DBTC business tables.

CREATE TABLE rwl_import_run (
    run_id            NUMBER(19)       NOT NULL,
    started_at_utc    TIMESTAMP        NOT NULL,
    finished_at_utc   TIMESTAMP,
    mode_code         VARCHAR2(16)     NOT NULL,
    status_code       VARCHAR2(32)     NOT NULL,
    request_summary   VARCHAR2(1000),
    error_summary     VARCHAR2(2000),
    wagons_seen       NUMBER(10)       DEFAULT 0 NOT NULL,
    ops_seen          NUMBER(10)       DEFAULT 0 NOT NULL,
    inserted_cnt      NUMBER(10)       DEFAULT 0 NOT NULL,
    updated_cnt       NUMBER(10)       DEFAULT 0 NOT NULL,
    unchanged_cnt     NUMBER(10)       DEFAULT 0 NOT NULL,
    rejected_cnt      NUMBER(10)       DEFAULT 0 NOT NULL,
    CONSTRAINT pk_rwl_import_run PRIMARY KEY (run_id)
);

CREATE SEQUENCE rwl_import_run_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE rwl_staging_operation (
    operation_id         NUMBER(19)       NOT NULL,
    run_id               NUMBER(19)       NOT NULL,
    event_key            VARCHAR2(512)    NOT NULL,
    content_hash         VARCHAR2(64)     NOT NULL,
    revision_no          NUMBER(10)       NOT NULL,
    wagon_no             VARCHAR2(32)     NOT NULL,
    station_code         VARCHAR2(16),
    station_name         VARCHAR2(256),
    operation_time_utc   TIMESTAMP        NOT NULL,
    operation_code       VARCHAR2(64),
    operation_asoup_code VARCHAR2(16),
    operation_name       VARCHAR2(256),
    train_index          VARCHAR2(64),
    train_number         VARCHAR2(64),
    from_station_code    VARCHAR2(16),
    from_station_name    VARCHAR2(256),
    to_station_code      VARCHAR2(16),
    to_station_name      VARCHAR2(256),
    loaded_flag          VARCHAR2(8),
    cargo_name           VARCHAR2(512),
    etsng_code           VARCHAR2(32),
    weight_tons          VARCHAR2(32),
    latitude             VARCHAR2(64),
    longitude            VARCHAR2(64),
    source_code          VARCHAR2(16)     DEFAULT 'RWL' NOT NULL,
    fetched_at_utc       TIMESTAMP        NOT NULL,
    created_at_utc       TIMESTAMP        NOT NULL,
    CONSTRAINT pk_rwl_staging_operation PRIMARY KEY (operation_id),
    CONSTRAINT fk_rwl_op_run FOREIGN KEY (run_id) REFERENCES rwl_import_run (run_id),
    CONSTRAINT uq_rwl_op_event_rev UNIQUE (event_key, revision_no)
);

CREATE SEQUENCE rwl_staging_operation_seq START WITH 1 INCREMENT BY 1 NOCACHE;

-- Points at the current revision for an event. PK enforces one current row per event.
CREATE TABLE rwl_staging_current (
    event_key      VARCHAR2(512) NOT NULL,
    operation_id   NUMBER(19)    NOT NULL,
    content_hash   VARCHAR2(64)  NOT NULL,
    updated_at_utc TIMESTAMP     NOT NULL,
    CONSTRAINT pk_rwl_staging_current PRIMARY KEY (event_key),
    CONSTRAINT fk_rwl_cur_op FOREIGN KEY (operation_id) REFERENCES rwl_staging_operation (operation_id)
);

CREATE TABLE rwl_staging_reject (
    reject_id        NUMBER(19)    NOT NULL,
    run_id           NUMBER(19)    NOT NULL,
    wagon_no         VARCHAR2(32),
    reason_code      VARCHAR2(64)  NOT NULL,
    detail_text      VARCHAR2(2000),
    raw_excerpt      VARCHAR2(2000),
    fetched_at_utc   TIMESTAMP     NOT NULL,
    created_at_utc   TIMESTAMP     NOT NULL,
    CONSTRAINT pk_rwl_staging_reject PRIMARY KEY (reject_id),
    CONSTRAINT fk_rwl_rej_run FOREIGN KEY (run_id) REFERENCES rwl_import_run (run_id)
);

CREATE SEQUENCE rwl_staging_reject_seq START WITH 1 INCREMENT BY 1 NOCACHE;
