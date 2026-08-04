-- Kích hoạt PostgreSQL extension unaccent dùng cho tìm kiếm không dấu
-- (bỏ dấu tiếng Việt trước khi so sánh LIKE / toan tu). Idempotent: chạy lại an toàn.
CREATE EXTENSION IF NOT EXISTS unaccent;
