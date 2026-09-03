-- ============================================================================
-- V1: Khởi tạo toàn bộ schema cho core-service
-- ============================================================================
-- Bảng này KHÔNG được quản lý bởi ddl-auto (đã chuyển sang `none`).
-- -> Flyway là nguồn quản lý schema DUY NHẤT. Khi tạo database mới, toàn bộ
--    bảng + index + FK được tạo từ đây, các migration sau (V2..V28) chỉ bổ sung
--    cột / seed data / chỉnh sửa.
--
-- Toàn bộ file dùng `IF NOT EXISTS` để an toàn khi chạy lại trên DB cũ
-- (schema đã có sẵn do Hibernate tự sinh trước đây) — không phá dữ liệu.
-- Cột được đối chiếu 1-1 với entity trong ai/entity/postgres.
-- ============================================================================

-- ────────────────────────────────────────────────────────────────────────────
-- 1. USERS
-- ────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.users (
    id             uuid         NOT NULL,
    user_name      varchar(255) NOT NULL,
    password       varchar(255),
    first_name     varchar(255) NOT NULL,
    last_name      varchar(255),
    gender         int4         NOT NULL,
    email          varchar(255) NOT NULL,
    phone_number   varchar(255),
    active         bool         NOT NULL,
    last_login     timestamptz,
    source         varchar(255) NOT NULL,
    login_attempts int4         NOT NULL DEFAULT 0,
    locked_until   timestamptz,
    created_at     timestamptz,
    created_by     uuid,
    updated_at     timestamptz,
    updated_by     uuid,
    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT uk_users_user_name UNIQUE (user_name)
);

-- ────────────────────────────────────────────────────────────────────────────
-- 2. ORGANIZATIONS (tự tham chiếu parent_id)
-- ────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.organizations (
    id          uuid         NOT NULL,
    name        varchar(255) NOT NULL,
    description varchar(255),
    path        varchar(255),
    parent_id   uuid,
    created_at  timestamptz,
    created_by  uuid,
    updated_at  timestamptz,
    updated_by  uuid,
    CONSTRAINT organizations_pkey PRIMARY KEY (id),
    CONSTRAINT fk_organizations_parent FOREIGN KEY (parent_id)
        REFERENCES public.organizations (id)
);

-- ────────────────────────────────────────────────────────────────────────────
-- 3. ROLE
-- ────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.role (
    id             uuid         NOT NULL,
    name           varchar(255) NOT NULL,
    description    varchar(255),
    default_assign bool,
    created_at     timestamptz,
    created_by     uuid,
    updated_at     timestamptz,
    updated_by     uuid,
    CONSTRAINT role_pkey PRIMARY KEY (id)
);

-- ────────────────────────────────────────────────────────────────────────────
-- 4. PERMISSION (scopes dạng text[] giống Hibernate @JdbcTypeCode(ARRAY))
-- ────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.permission (
    id              uuid         NOT NULL,
    name            varchar(255) NOT NULL,
    description     varchar(255),
    code            varchar(255) NOT NULL,
    resource        varchar(255) NOT NULL,
    action          varchar(255) NOT NULL,
    target_resource varchar(255),
    scopes          text[],
    created_at      timestamptz,
    created_by      uuid,
    updated_at      timestamptz,
    updated_by      uuid,
    CONSTRAINT permission_pkey PRIMARY KEY (id)
);

-- ────────────────────────────────────────────────────────────────────────────
-- 5. ROLE_PERMISSIONS (PK ghép role_id + permission_id)
-- ────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.role_permissions (
    role_id       uuid NOT NULL,
    permission_id uuid NOT NULL,
    scope         varchar(255),
    CONSTRAINT role_permissions_pkey PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id)
        REFERENCES public.role (id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id)
        REFERENCES public.permission (id)
);

-- ────────────────────────────────────────────────────────────────────────────
-- 6. ORGANIZATION_USER_ROLE (PK ghép organization_id + user_id + role_id)
-- ────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.organization_user_role (
    organization_id uuid NOT NULL,
    user_id         uuid NOT NULL,
    role_id         uuid NOT NULL,
    CONSTRAINT organization_user_role_pkey PRIMARY KEY (organization_id, user_id, role_id),
    CONSTRAINT fk_our_organization FOREIGN KEY (organization_id)
        REFERENCES public.organizations (id),
    CONSTRAINT fk_our_user FOREIGN KEY (user_id)
        REFERENCES public.users (id),
    CONSTRAINT fk_our_role FOREIGN KEY (role_id)
        REFERENCES public.role (id)
);

-- ────────────────────────────────────────────────────────────────────────────
-- 7. MESSAGE (ID do app sinh UUIDv7 qua @UuidV7)
-- ────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.message (
    id                uuid NOT NULL,
    content           text NOT NULL,
    type              varchar(255) NOT NULL,
    source            text,
    feedback          varchar(255),
    suggested_replies text,
    reasoning_steps   text,
    created_at        timestamptz,
    created_by        uuid,
    updated_at        timestamptz,
    updated_by        uuid,
    CONSTRAINT message_pkey PRIMARY KEY (id)
);

-- ────────────────────────────────────────────────────────────────────────────
-- 8. MESSAGE_FEEDBACK_HISTORY
-- ────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.message_feedback_history (
    id              uuid NOT NULL,
    message_id      uuid NOT NULL,
    before_feedback varchar(255),
    after_feedback  varchar(255),
    created_at      timestamptz,
    created_by      uuid,
    updated_at      timestamptz,
    updated_by      uuid,
    CONSTRAINT message_feedback_history_pkey PRIMARY KEY (id),
    CONSTRAINT fk_mfh_message FOREIGN KEY (message_id)
        REFERENCES public.message (id)
);

CREATE INDEX IF NOT EXISTS idx_message_feedback_history_message_id
    ON public.message_feedback_history (message_id);
CREATE INDEX IF NOT EXISTS idx_message_feedback_history_created_at
    ON public.message_feedback_history (created_at);

-- ────────────────────────────────────────────────────────────────────────────
-- 9. TOPIC
-- ────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.topic (
    id                                 uuid         NOT NULL,
    title                              varchar(255) NOT NULL,
    type                               varchar(255) NOT NULL,
    conversation_summary               text,
    conversation_summary_last_message_id uuid,
    owner_id                           uuid,
    organization_id                    uuid,
    created_at                         timestamptz,
    created_by                         uuid,
    updated_at                         timestamptz,
    updated_by                         uuid,
    CONSTRAINT topic_pkey PRIMARY KEY (id),
    CONSTRAINT fk_topic_owner FOREIGN KEY (owner_id)
        REFERENCES public.users (id),
    CONSTRAINT fk_topic_organization FOREIGN KEY (organization_id)
        REFERENCES public.organizations (id)
);

-- ────────────────────────────────────────────────────────────────────────────
-- 10. NOTEBOOK
-- ────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.notebook (
    id                                 uuid NOT NULL,
    title                              varchar(255) NOT NULL,
    description                        text NOT NULL,
    instruction                        text,
    conversation_summary               text,
    conversation_summary_last_message_id uuid,
    owner_id                           uuid,
    organization_id                    uuid,
    created_at                         timestamptz,
    created_by                         uuid,
    updated_at                         timestamptz,
    updated_by                         uuid,
    CONSTRAINT notebook_pkey PRIMARY KEY (id),
    CONSTRAINT fk_notebook_owner FOREIGN KEY (owner_id)
        REFERENCES public.users (id),
    CONSTRAINT fk_notebook_organization FOREIGN KEY (organization_id)
        REFERENCES public.organizations (id)
);

-- ────────────────────────────────────────────────────────────────────────────
-- 11. NOTE (topic_id / notebook_id là cột UUID trần, không phải FK)
-- ────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.note (
    id          uuid NOT NULL,
    title       varchar(255),
    content     text NOT NULL,
    source_type varchar(255),
    source_by   varchar(255) NOT NULL,
    topic_id    uuid,
    notebook_id uuid,
    owner_id    uuid,
    org_id      uuid,
    created_at  timestamptz,
    created_by  uuid,
    updated_at  timestamptz,
    updated_by  uuid,
    CONSTRAINT note_pkey PRIMARY KEY (id),
    CONSTRAINT fk_note_owner FOREIGN KEY (owner_id)
        REFERENCES public.users (id),
    CONSTRAINT fk_note_organization FOREIGN KEY (org_id)
        REFERENCES public.organizations (id)
);

CREATE INDEX IF NOT EXISTS idx_note_owner_id    ON public.note (owner_id);
CREATE INDEX IF NOT EXISTS idx_note_source_type ON public.note (source_type);
CREATE INDEX IF NOT EXISTS idx_note_topic_id    ON public.note (topic_id);
CREATE INDEX IF NOT EXISTS idx_note_notebook_id ON public.note (notebook_id);

-- ────────────────────────────────────────────────────────────────────────────
-- 12. DRAFT
-- ────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.draft (
    id                     uuid NOT NULL,
    type                   varchar(255) NOT NULL,
    scopes                 text[],
    format_standard        varchar(255),
    title                  varchar(255) NOT NULL,
    detailed_description   text,
    latest_version_number  int4 NOT NULL,
    latest_content_preview text,
    session_id             varchar(255),
    owner_id               uuid,
    organization_id        uuid,
    created_at             timestamptz,
    created_by             uuid,
    updated_at             timestamptz,
    updated_by             uuid,
    CONSTRAINT draft_pkey PRIMARY KEY (id),
    CONSTRAINT fk_draft_owner FOREIGN KEY (owner_id)
        REFERENCES public.users (id),
    CONSTRAINT fk_draft_organization FOREIGN KEY (organization_id)
        REFERENCES public.organizations (id)
);

CREATE INDEX IF NOT EXISTS idx_draft_owner_id    ON public.draft (owner_id);
CREATE INDEX IF NOT EXISTS idx_draft_org_id      ON public.draft (organization_id);
CREATE INDEX IF NOT EXISTS idx_draft_updated_at  ON public.draft (updated_at);

-- ────────────────────────────────────────────────────────────────────────────
-- 13. DRAFT_VERSION
-- ────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.draft_version (
    id                   uuid NOT NULL,
    draft_id             uuid NOT NULL,
    version_number       int4 NOT NULL,
    detailed_description text NOT NULL,
    change_request       text,
    generated_content    text NOT NULL,
    created_at           timestamptz,
    created_by           uuid,
    updated_at           timestamptz,
    updated_by           uuid,
    CONSTRAINT draft_version_pkey PRIMARY KEY (id),
    CONSTRAINT uk_draft_version_draft_id_version_number UNIQUE (draft_id, version_number),
    CONSTRAINT fk_draft_version_draft FOREIGN KEY (draft_id)
        REFERENCES public.draft (id)
);

CREATE INDEX IF NOT EXISTS idx_draft_version_draft_id   ON public.draft_version (draft_id);
CREATE INDEX IF NOT EXISTS idx_draft_version_created_at ON public.draft_version (created_at);

-- ────────────────────────────────────────────────────────────────────────────
-- 14. TOPIC_MESSAGES / NOTEBOOK_MESSAGES / DRAFT_MESSAGES (bảng nối + message)
-- ────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.topic_messages (
    topic_id   uuid NOT NULL,
    message_id uuid NOT NULL,
    CONSTRAINT topic_messages_pkey PRIMARY KEY (topic_id, message_id),
    CONSTRAINT fk_topic_messages_topic   FOREIGN KEY (topic_id)   REFERENCES public.topic (id),
    CONSTRAINT fk_topic_messages_message FOREIGN KEY (message_id) REFERENCES public.message (id)
);

CREATE TABLE IF NOT EXISTS public.notebook_messages (
    notebook_id uuid NOT NULL,
    message_id  uuid NOT NULL,
    CONSTRAINT notebook_messages_pkey PRIMARY KEY (notebook_id, message_id),
    CONSTRAINT fk_notebook_messages_notebook FOREIGN KEY (notebook_id) REFERENCES public.notebook (id),
    CONSTRAINT fk_notebook_messages_message  FOREIGN KEY (message_id)  REFERENCES public.message (id)
);

CREATE TABLE IF NOT EXISTS public.draft_messages (
    draft_id   uuid NOT NULL,
    message_id uuid NOT NULL,
    CONSTRAINT draft_messages_pkey PRIMARY KEY (draft_id, message_id),
    CONSTRAINT fk_draft_messages_draft   FOREIGN KEY (draft_id)   REFERENCES public.draft (id),
    CONSTRAINT fk_draft_messages_message FOREIGN KEY (message_id) REFERENCES public.message (id)
);

-- ────────────────────────────────────────────────────────────────────────────
-- 15. TOPIC_SOURCES
-- ────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.topic_sources (
    id           uuid NOT NULL,
    topic_id     uuid NOT NULL,
    source_type  varchar(255) NOT NULL,
    display_name varchar(255) NOT NULL,
    raw_content  text,
    file_path    varchar(255),
    summary      text,
    metadata     text,
    vector_status varchar(255) NOT NULL,
    created_at   timestamptz,
    created_by   uuid,
    updated_at   timestamptz,
    updated_by   uuid,
    CONSTRAINT topic_sources_pkey PRIMARY KEY (id),
    CONSTRAINT fk_topic_sources_topic FOREIGN KEY (topic_id)
        REFERENCES public.topic (id)
);

CREATE INDEX IF NOT EXISTS idx_topic_sources_topic_id ON public.topic_sources (topic_id);

-- ────────────────────────────────────────────────────────────────────────────
-- 16. NOTEBOOK_SOURCES
-- ────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.notebook_sources (
    id                  uuid NOT NULL,
    notebook_id         uuid NOT NULL,
    note_id             uuid,
    source_type         varchar(255) NOT NULL,
    display_name        varchar(255) NOT NULL,
    raw_content         text,
    file_path           varchar(255),
    summary             text,
    metadata            text,
    vector_status       varchar(255) NOT NULL,
    job_id              uuid,
    owner_id            uuid,
    org_id              uuid,
    delete_status       varchar(20) NOT NULL DEFAULT 'ACTIVE',
    dispatch_retry_count int4 DEFAULT 0,
    delete_retry_count   int4 DEFAULT 0,
    created_at          timestamptz,
    created_by          uuid,
    updated_at          timestamptz,
    updated_by          uuid,
    CONSTRAINT notebook_sources_pkey PRIMARY KEY (id),
    CONSTRAINT fk_notebook_sources_notebook FOREIGN KEY (notebook_id)
        REFERENCES public.notebook (id),
    CONSTRAINT fk_notebook_sources_note FOREIGN KEY (note_id)
        REFERENCES public.note (id)
);

CREATE INDEX IF NOT EXISTS idx_notebook_sources_notebook_id  ON public.notebook_sources (notebook_id);
CREATE INDEX IF NOT EXISTS idx_notebook_sources_note_id      ON public.notebook_sources (note_id);
CREATE INDEX IF NOT EXISTS idx_notebook_sources_job_id       ON public.notebook_sources (job_id);
CREATE INDEX IF NOT EXISTS idx_notebook_sources_delete_status ON public.notebook_sources (delete_status);
CREATE INDEX IF NOT EXISTS idx_notebook_sources_owner_id     ON public.notebook_sources (owner_id);
CREATE INDEX IF NOT EXISTS idx_notebook_sources_org_id       ON public.notebook_sources (org_id);

-- ────────────────────────────────────────────────────────────────────────────
-- 17. DATA_INGESTION (tự tham chiếu parent_id)
-- ────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.data_ingestion (
    id               uuid NOT NULL,
    name             varchar(255) NOT NULL,
    folder           bool NOT NULL,
    content_type     varchar(120),
    file_size        int8,
    minio_path       varchar(255),
    parent_id        uuid,
    owner_id         uuid,
    org_id           uuid,
    access_level     varchar(20),
    from_source      varchar(20),
    job_id           uuid,
    ingestion_status varchar(20),
    delete_status    varchar(20) NOT NULL DEFAULT 'ACTIVE',
    retry_count      int4 NOT NULL DEFAULT 0,
    ingestion_error  text,
    created_at       timestamptz,
    created_by       uuid,
    updated_at       timestamptz,
    updated_by       uuid,
    CONSTRAINT data_ingestion_pkey PRIMARY KEY (id),
    CONSTRAINT fk_data_ingestion_parent FOREIGN KEY (parent_id)
        REFERENCES public.data_ingestion (id),
    CONSTRAINT fk_data_ingestion_owner FOREIGN KEY (owner_id)
        REFERENCES public.users (id),
    CONSTRAINT fk_data_ingestion_organization FOREIGN KEY (org_id)
        REFERENCES public.organizations (id)
);

CREATE INDEX IF NOT EXISTS idx_data_ingestion_folder           ON public.data_ingestion (folder);
CREATE INDEX IF NOT EXISTS idx_data_ingestion_org_id           ON public.data_ingestion (org_id);
CREATE INDEX IF NOT EXISTS idx_data_ingestion_owner_id         ON public.data_ingestion (owner_id);
CREATE INDEX IF NOT EXISTS idx_data_ingestion_parent_id        ON public.data_ingestion (parent_id);
CREATE INDEX IF NOT EXISTS idx_data_ingestion_ingestion_status ON public.data_ingestion (ingestion_status);
CREATE INDEX IF NOT EXISTS idx_data_ingestion_delete_status    ON public.data_ingestion (delete_status);
CREATE INDEX IF NOT EXISTS idx_data_ingestion_access_level     ON public.data_ingestion (access_level);
CREATE INDEX IF NOT EXISTS idx_data_ingestion_from_source      ON public.data_ingestion (from_source);

-- ────────────────────────────────────────────────────────────────────────────
-- 18. AUDIT_LOGS (không dùng AuditEmbed — chỉ có created_at @PrePersist)
--     LƯU Ý: cố ý KHÔNG tạo CHECK constraint cho cột enum (action/resource/
--     status) — xem V17 (drop check constraints, source of truth là Java enum).
-- ────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.audit_logs (
    id                uuid NOT NULL,
    action            varchar(64)  NOT NULL,
    resource          varchar(64)  NOT NULL,
    resource_id       varchar(128),
    resource_name     varchar(512),
    description       varchar(1024),
    user_id           uuid,
    user_name         varchar(256),
    org_id            uuid,
    organization_name varchar(256),
    ip_address        varchar(64),
    user_agent        varchar(512),
    method            varchar(128),
    path              varchar(512),
    status            varchar(32) NOT NULL,
    details           jsonb,
    error_message     varchar(2048),
    created_at        timestamptz NOT NULL,
    CONSTRAINT audit_logs_pkey PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_audit_log_user       ON public.audit_logs (user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_org        ON public.audit_logs (org_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_action     ON public.audit_logs (action);
CREATE INDEX IF NOT EXISTS idx_audit_log_resource   ON public.audit_logs (resource);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON public.audit_logs (created_at);
CREATE INDEX IF NOT EXISTS idx_audit_log_status     ON public.audit_logs (status);