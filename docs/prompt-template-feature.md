# Tính năng: Quick Prompt Template

## Mục đích

Lưu các câu prompt/input nhanh thường dùng khi chat với **Topic** và **NotebookLM**, giúp người dùng:
- Lấy nhanh prompt để bắt đầu một phiên chat mới.
- Dùng lại prompt trong chính phiên chat (input tiếp theo).

Có 2 nguồn prompt:
- **System prompt (scope = SYSTEM)** — do admin tạo, dùng chung cho **tất cả org** (global).
- **User prompt (scope = USER)** — do người dùng tự tạo, chỉ hiển thị cho chính user đó (trong org của user).

## Phân loại (PromptType)

| Giá trị  | Ý nghĩa                                  |
|----------|------------------------------------------|
| `TOPIC`  | Dùng cho chat với Topic.                 |
| `NOTEBOOK` | Dùng cho chat với NotebookLM.          |

> **Mỗi prompt chỉ thuộc đúng 1 loại.** Prompt dùng chung cho cả Topic lẫn Notebook thì tạo **2 record riêng** (1 `TOPIC` + 1 `NOTEBOOK`) với cùng `content`.
>
> Trước đây có giá trị gộp `BOTH` nhưng đã **bỏ** vì: (1) lọc `promptType=TOPIC` sẽ làm mất luôn prompt `BOTH` do logic dùng `equal`; (2) không mở rộng được — nếu thêm loại chatbot thứ 3 thì `BOTH` không còn nghĩa "dùng cho tất cả". Migration `V34` tách dữ liệu `BOTH` cũ thành 2 record tương ứng.

## Luồng truy cập

### User flow (`/user/prompt-templates`)
- `GET /` — list **system prompt (active)** + **user prompt của mình (active)**. Filter: `promptType`, `scope`, `keyword` (tìm trong title/content).
- `GET /{id}` — xem prompt của mình hoặc system prompt.
- `POST /` — tạo prompt cá nhân (scope=USER, gắn owner + org từ JWT).
- `PUT /{id}` — cập nhật prompt của mình (partial update — field null giữ nguyên).
- `DELETE /{id}` — xóa prompt của mình.
- `GET /types` — danh sách PromptType.

### Admin flow (`/admin/prompt-templates`)
> Bảo vệ bằng `@PreAuthorize("@adminAccessGuard.isAllowed()")` (whitelist username, giống System Setting).

- `GET /` — list **tất cả prompt**: system prompt (global) + user prompt trong org của admin. Filter: `promptType`, `scope`, `isActive`, `keyword`.
- `GET /{id}` — xem chi tiết (system hoặc user prompt trong org).
- `POST /` — tạo **system prompt** (global, không gắn owner/org).
- `PUT /{id}` — cập nhật prompt bất kỳ (kể cả của user, trong org của admin).
- `DELETE /{id}` — xóa prompt bất kỳ.
- `GET /types` — danh sách PromptType.
- `GET /access` — kiểm tra token hiện tại có quyền truy cập admin APIs (trả `Boolean`, không cần `@PreAuthorize`).

### Dùng chung (`/category`)
- `GET /category/prompt-types` — danh sách PromptType (TOPIC / NOTEBOOK) cho mọi authenticated user.

## Quy tắc nghiệp vụ

1. **System prompt global**: không có `owner`, không có `organization`. Mọi user mọi org đều thấy (nếu `isActive = true`).
2. **User prompt**: gắn `owner` + `organization`. Chỉ owner mới sửa/xóa được (ownership check qua `existsByIdAndOwnerId`).
3. **Admin chỉ truy cập được**: system prompt (global) hoặc user prompt **trong org của admin** — không chạm được prompt user của org khác.
4. **`isActive`**: system prompt bị tắt sẽ ẩn khỏi danh sách user. User prompt bị admin tắt cũng ẩn khỏi danh sách của user.
5. **Display order**: nhỏ trước, lớn sau — sort theo `displayOrder` ASC.

## Cấu trúc code

| Layer | File |
|-------|------|
| Entity | `ai/entity/postgres/PromptTemplateEntity.java` |
| Repository | `ai/repository/PromptTemplateRepository.java` |
| Enums | `ai/enums/PromptType.java`, `ai/enums/PromptScope.java` |
| Request DTO | `ai/dto/own/request/PromptTemplateCreateRequestDto.java`, `PromptTemplateUpdateRequestDto.java` |
| Filter DTO | `ai/dto/own/request/filter/PromptTemplateFilterDto.java` |
| Response DTO | `ai/dto/own/response/PromptTemplateResponseDto.java` |
| Mapper | `ai/mapper/PromptTemplateMapper.java` (extends `GeneralMapper`, partial update) |
| Service | `ai/service/PromptTemplateService.java` |
| Controller | `ai/controller/admin/PromptTemplateAdminController.java`, `ai/controller/user/PromptTemplateUserController.java` |
| Migration | `db/migration/V22__create_prompt_templates.sql` (kèm seed 15 system prompt) |

## Error codes

`ApiResponseStatus` dải **1182–1186** (`PROMPT_TEMPLATE_*`). Xem comment section header trong enum.

## Seed data

Migration `V22` chèn sẵn **15 system prompt** (5 TOPIC, 5 NOTEBOOK, 5 BOTH) bằng UUID cố định + `ON CONFLICT DO NOTHING` (idempotent), để luôn có mẫu khi deploy.

> Migration `V34` đã tách 5 prompt `BOTH` đó thành 5 `TOPIC` + 5 `NOTEBOOK` (giữ nguyên nội dung). `V22` giữ nguyên giá trị lịch sử, `V34` là bước migrate dữ liệu.
