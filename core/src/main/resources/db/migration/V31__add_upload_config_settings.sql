-- Thêm cấu hình giới hạn upload file cho từng loại (Topic/Notebook/Draft/Data-Ingestion)
-- Thuộc nhóm UPLOAD, is_public=true (FE đọc qua /public/settings để chặn tại giao diện)
-- Quy ước: giá trị = 0 nghĩa là không giới hạn (unlimited)
INSERT INTO public.system_settings (id, created_at, created_by, updated_at, updated_by, description, display_order, is_active, is_public, setting_group, setting_key, setting_type, setting_value) VALUES
    -- Topic
    ('a0000007-0000-4000-8000-000000000001'::uuid, now(), NULL, now(), NULL, 'Số lượng file tối đa mỗi lần upload trong chat Topic (0 = không giới hạn)', 1, true, true, 'UPLOAD', 'upload.topic.maxFileCount', 'INTEGER', '3'),
    ('a0000007-0000-4000-8000-000000000002'::uuid, now(), NULL, now(), NULL, 'Dung lượng tối đa mỗi file (MB) khi upload trong chat Topic (0 = không giới hạn)', 2, true, true, 'UPLOAD', 'upload.topic.maxFileSizeMb', 'INTEGER', '100'),
    ('a0000007-0000-4000-8000-000000000003'::uuid, now(), NULL, now(), NULL, 'Danh sách extension file được phép upload trong chat Topic (phân tách bởi dấu phẩy, rỗng = cho phép tất cả)', 3, true, true, 'UPLOAD', 'upload.topic.allowedFileTypes', 'STRING', ''),
    -- Notebook
    ('a0000007-0000-4000-8000-000000000004'::uuid, now(), NULL, now(), NULL, 'Số lượng file tối đa mỗi lần upload trong chat Notebook (0 = không giới hạn)', 4, true, true, 'UPLOAD', 'upload.notebook.maxFileCount', 'INTEGER', '10'),
    ('a0000007-0000-4000-8000-000000000005'::uuid, now(), NULL, now(), NULL, 'Dung lượng tối đa mỗi file (MB) khi upload trong chat Notebook (0 = không giới hạn)', 5, true, true, 'UPLOAD', 'upload.notebook.maxFileSizeMb', 'INTEGER', '100'),
    ('a0000007-0000-4000-8000-000000000006'::uuid, now(), NULL, now(), NULL, 'Tổng số source tối đa được phép sử dụng trong mỗi Notebook (0 = không giới hạn)', 6, true, true, 'UPLOAD', 'upload.notebook.maxTotalSources', 'INTEGER', '100'),
    ('a0000007-0000-4000-8000-000000000007'::uuid, now(), NULL, now(), NULL, 'Danh sách extension file được phép upload trong chat Notebook (phân tách bởi dấu phẩy, rỗng = cho phép tất cả)', 7, true, true, 'UPLOAD', 'upload.notebook.allowedFileTypes', 'STRING', ''),
    -- Draft
    ('a0000007-0000-4000-8000-000000000008'::uuid, now(), NULL, now(), NULL, 'Số lượng file tối đa mỗi lần upload trong Draft (0 = không giới hạn)', 8, true, true, 'UPLOAD', 'upload.draft.maxFileCount', 'INTEGER', '3'),
    ('a0000007-0000-4000-8000-000000000009'::uuid, now(), NULL, now(), NULL, 'Dung lượng tối đa mỗi file (MB) khi upload trong Draft (0 = không giới hạn)', 9, true, true, 'UPLOAD', 'upload.draft.maxFileSizeMb', 'INTEGER', '100'),
    ('a0000007-0000-4000-8000-000000000010'::uuid, now(), NULL, now(), NULL, 'Danh sách extension file được phép upload trong Draft (phân tách bởi dấu phẩy, rỗng = cho phép tất cả)', 10, true, true, 'UPLOAD', 'upload.draft.allowedFileTypes', 'STRING', ''),
    -- Data-Ingestion
    ('a0000007-0000-4000-8000-000000000011'::uuid, now(), NULL, now(), NULL, 'Số lượng file tối đa mỗi lần upload trong Data-Ingestion (0 = không giới hạn)', 11, true, true, 'UPLOAD', 'upload.dataIngestion.maxFileCount', 'INTEGER', '10'),
    ('a0000007-0000-4000-8000-000000000012'::uuid, now(), NULL, now(), NULL, 'Dung lượng tối đa mỗi file (MB) khi upload trong Data-Ingestion (0 = không giới hạn)', 12, true, true, 'UPLOAD', 'upload.dataIngestion.maxFileSizeMb', 'INTEGER', '100'),
    ('a0000007-0000-4000-8000-000000000013'::uuid, now(), NULL, now(), NULL, 'Danh sách extension file được phép upload trong Data-Ingestion (phân tách bởi dấu phẩy, rỗng = cho phép tất cả)', 13, true, true, 'UPLOAD', 'upload.dataIngestion.allowedFileTypes', 'STRING', '')
ON CONFLICT (setting_key) DO NOTHING;
