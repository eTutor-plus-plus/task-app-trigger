CREATE TABLE task_group
(
    id                      NUMBER(19, 0)   NOT NULL,
    status                  VARCHAR2(20)    NOT NULL,
    -- custom task group columns
    ddl_statements          NCLOB           NOT NULL,
    diagnose_dml_statements NCLOB           NOT NULL,
    submit_dml_statements   NCLOB           NOT NULL,
    schema_description      JSON,
    -- Database
    schema_name             VARCHAR2 (200)  NOT NULL,
    CONSTRAINT task_group_pk PRIMARY KEY (id),
    CONSTRAINT task_group_status_ck CHECK (status IN ('draft', 'ready_for_approval', 'approved'))
);

CREATE TABLE task_group_query
(
    id            NUMBER(19, 0)     GENERATED ALWAYS AS IDENTITY,
    task_group_id NUMBER (19, 0)    NOT NULL,
    table_name    VARCHAR (255)     NOT NULL,
    query         NCLOB             NOT NULL,
    CONSTRAINT task_group_query_pk PRIMARY KEY (id),
    CONSTRAINT task_group_query_task_group_fk FOREIGN KEY (task_group_id) REFERENCES task_group (id)
        ON DELETE CASCADE
);

CREATE TABLE task
(
    id                  NUMBER(19, 0)   NOT NULL,
    max_points          NUMERIC(7, 2)   NOT NULL,
    status              VARCHAR2(18)    NOT NULL,
    task_group_id       NUMBER(19, 0)   NOT NULL,
    -- custom task columns
    solution            NCLOB           NOT NULL,
    trigger_Operations  NCLOB           NOT NULL,
    result_Tables       VARCHAR2(255)   NOT NULL,
    buffered            VARCHAR2 (10)   NOT NULL,
    comparison_Execution VARCHAR2 (10)   NOT NULL,
    wrong_Head_Penalty    NUMERIC(5, 2)   DEFAULT -1, -- -1 = solution is fully invalid
    wrong_Body_Penalty    NUMERIC(5, 2)   DEFAULT -0.75,
    CONSTRAINT task_pk PRIMARY KEY (id),
    CONSTRAINT task_status_ck       CHECK (status IN ('draft', 'ready_for_approval', 'approved')),
    CONSTRAINT task_task_group_fk   FOREIGN KEY (task_group_id) REFERENCES task_group (id)
        ON DELETE CASCADE
);

CREATE TABLE submission
(
    id                VARCHAR2(36)                NOT NULL,
    user_id           VARCHAR2(255),
    assignment_id     VARCHAR2(255),
    task_id           NUMBER(19, 0),
    submission_time   DATE        DEFAULT SYSDATE NOT NULL,
    language          VARCHAR2(2) DEFAULT 'en'    NOT NULL,
    "MODE"            VARCHAR2(8)                 NOT NULL,
    feedback_level    INT                         NOT NULL,
    evaluation_result JSON,
    -- TODO add custom columns with submission data
    CONSTRAINT submission_pk PRIMARY KEY (id),
    CONSTRAINT submission_mode_ck CHECK ("MODE" IN ('run', 'diagnose', 'submit')),
    CONSTRAINT submission_task_fk FOREIGN KEY (task_id) REFERENCES task (id)
        ON DELETE CASCADE
);
