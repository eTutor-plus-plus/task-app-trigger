CREATE TABLE task_group
(
    id     NUMBER(19, 0) NOT NULL,
    status VARCHAR2(18)  NOT NULL,
    -- TODO: add custom task group columns
    CONSTRAINT task_group_pk PRIMARY KEY (id),
    CONSTRAINT task_group_status_ck CHECK (status IN ('draft', 'ready_for_approval', 'approved'))
);

CREATE TABLE task
(
    id            NUMBER(19, 0) NOT NULL,
    max_points    NUMERIC(7, 2) NOT NULL,
    status        VARCHAR2(18)  NOT NULL,
    task_group_id NUMBER(19, 0) NOT NULL,
    -- TODO: add custom task columns
    CONSTRAINT task_pk PRIMARY KEY (id),
    CONSTRAINT task_status_ck CHECK (status IN ('draft', 'ready_for_approval', 'approved')),
    CONSTRAINT task_task_group_fk FOREIGN KEY (task_group_id) REFERENCES task_group (id)
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
