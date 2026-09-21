# AI Question Autocomplete (gợi ý câu hỏi cho ô chat)

Ngày tạo: 2026-09-20 · Cập nhật kiến trúc: 2026-09-21

## Mục đích

Khi người dùng đang gõ câu hỏi vào ô chat, FE gọi API để nhận danh sách câu hỏi hoàn chỉnh
gợi ý (kiểu `ask-autocomplete`). Người dùng bấm vào gợi ý → text trong ô nhập được **thay thế
bằng câu hoàn chỉnh** đó.

## Nguyên tắc thiết kế: dùng chung đường ống sinh text của hệ thống

Tác vụ này **không gọi model trực tiếp** và **không có LLM client riêng**. Nó đi qua đúng
cơ chế chung mà các tác vụ AI phụ trợ khác đang dùng (sinh tiêu đề, nén rolling summary):

```
AiSuggestionUserController   POST /user/ai/suggestions
        ↓
AiSuggestionService          guard prefix + Redis cache + semaphore + parse/filter
        ↓
RagService.generalQuestionsRaw(prompt)
        ↓
  buildSuggestionCompletionRequest(prompt)   ← tái dùng buildSimpleCompletionRequest + applyAiSettings
        ↓
RagApiService.general(dto, timeout)          ← POST /generate/v1/chat/completions_simple
        ↓
RagApiCore.postForString(endPoint, body, timeout)
        ↓
WebClient ragWebClient  (bean sẵn có, read-timeout 360s)
        ↓
RAG service  (quyết định model thực tế)
```

**Vì sao quan trọng:** việc chọn model nằm ở phía **RAG service**. Khi RAG bổ sung model
nhỏ/nhanh hơn, chỉ cần đổi System Setting `ai.suggestion.model` — core-service **không phải
sửa code hay deploy lại**.

Các tác vụ phụ trợ cũng được hưởng lợi từ cấu hình chung: `ai.model`, `ai.temperature`
(force `0.2` cho tác vụ tất định), `ai.maxTokens` — áp dụng nhất quán qua `applyAiSettings`.

> **Lịch sử:** bản đầu tiên (2026-09-20) tự dựng `OllamaApiCore` + bean `ollamaWebClient`
> + config `suggestion.*` trong YAML, gọi native Ollama `/api/chat`. Cách đó tạo ra **đường
> ống thứ hai song song** với đường ống sẵn có, và buộc core-service phải biết về Ollama.
> Đã refactor bỏ hoàn toàn (xem mục "Đã gỡ bỏ" ở cuối).

## API

### `POST /user/ai/suggestions`

Yêu cầu JWT (`/user/**` — filter chain `@Order(3)` của `SecurityConfig`, không cần chỉnh config).

Request:

```json
{ "prefix": "giá xăng dầu" }
```

Response (`ApiResponseModel<List<String>>`):

```json
{
  "status": 1000,
  "message": "Get suggestions successfully",
  "data": [
    "giá xăng dầu hiện nay có tăng hay giảm?",
    "giá xăng dầu ở khu vực miền Bắc so với miền Nam?",
    "giá xăng dầu dự kiến vào tháng tới sẽ như thế nào?",
    "giá xăng dầu ảnh hưởng tới giá nguyên liệu thực phẩm ra sao?",
    "giá xăng dầu trên thị trường quốc tế ảnh hưởng như thế nào?"
  ]
}
```

Đặc điểm contract:

| Trường hợp | Hành vi |
|---|---|
| Prefix quá ngắn (< `minPrefixChars`) | `data: []`, **không** gọi model |
| Hệ thống quá tải (hết semaphore permit) | `data: []`, HTTP 200 |
| RAG service down / timeout | `data: []`, HTTP 200, log WARN |
| Model trả output không parse được | `data: []`, HTTP 200 |
| **Redis down** | **vẫn sinh gợi ý bình thường**, chỉ bỏ qua cache (log DEBUG) |
| System Setting đọc lỗi | dùng default trong code, vẫn sinh gợi ý |
| `prefix` rỗng/null | HTTP 400, code `1206` |
| Tất cả gợi ý không bắt đầu bằng prefix | `data: []` (bị lọc sạch) |

> **Thiết kế:** endpoint này **không bao giờ trả lỗi 5xx**. Autocomplete là tính năng phụ
> trợ; khi lỗi thì im lặng trả rỗng để FE degrade êm.
>
> Toàn bộ thân `AiSuggestionService.suggest()` được bọc trong `try/catch` — **không chỉ
> riêng lời gọi model**. Ngoài model/RAG, luồng này còn phụ thuộc Redis (cache) và DB
> (System Setting); nếu chỉ bắt lỗi quanh lời gọi model thì Redis/DB down sẽ ném ra ngoài
> và `GlobalExceptionHandler` trả 500.
>
> **Cache là thành phần optional**: `readCache`/`writeCache`/`resolveLanguage` tự bắt lỗi
> và chỉ log DEBUG. Redis down làm mất phần tăng tốc nhưng **không** làm mất tính năng.

## Ràng buộc "gợi ý phải bắt đầu bằng prefix"

Mỗi gợi ý trả về **bắt buộc** bắt đầu bằng chính xác phần text người dùng đã gõ (không phân
biệt hoa/thường). Điều này cho phép FE thay thế text trong ô nhập bằng gợi ý (true
completion) mà không gây cảm giác "nhảy" nội dung.

Hệ quả cần lưu ý:

- Model trả gợi ý không khớp prefix → gợi ý đó bị **loại bỏ**, nên số lượng trả về có thể
  **ít hơn** `maxSuggestions`.
- Prefix dài hơn `maxPrefixChars` bị **cắt lấy phần đuôi** (phần người dùng vừa gõ chứa
  nhiều ngữ cảnh hơn). Bước kiểm tra prefix cũng dùng chính giá trị đã cắt này, nếu không
  mọi gợi ý sẽ bị lọc sạch.

## Cấu hình — System Settings (chỉnh nóng, không cần deploy)

Toàn bộ ngưỡng nghiệp vụ nằm trong **System Settings DB**, nhóm `AI`, `is_public=false`,
seed bởi migration `V36__add_ai_suggestion_settings.sql`. Key + default khai báo tập trung tại
`ai/constant/AiSuggestionConfig.java`.

Đọc qua `SystemSettingService` với quy ước chung của repo: **giá trị rỗng, `<= 0` hoặc không
parse được → dùng default trong code**.

| Key | Mặc định | Ý nghĩa |
|---|---|---|
| `ai.suggestion.model` | `''` (rỗng) | **Model riêng cho autocomplete. Rỗng = dùng chung `ai.model`** |
| `ai.suggestion.timeoutSeconds` | `15` | Timeout riêng cho lời gọi sinh gợi ý |
| `ai.suggestion.maxSuggestions` | `5` | Số gợi ý tối đa |
| `ai.suggestion.minPrefixChars` | `3` | Ngắn hơn → trả rỗng, không gọi model |
| `ai.suggestion.maxPrefixChars` | `200` | Cắt prefix trước khi đưa vào prompt |
| `ai.suggestion.maxSuggestionChars` | `120` | Độ dài tối đa mỗi gợi ý |
| `ai.suggestion.maxConcurrentRequests` | `4` | Giới hạn request đồng thời |

### Cách đổi model cho autocomplete

Khi RAG service có thêm model nhỏ/nhanh hơn:

1. Vào System Settings, set `ai.suggestion.model` = tên model mới (VD `model-mini`).
2. Không cần deploy core-service. Lần gọi tiếp theo đã dùng model mới
   (`SystemSettingService` có `@Cacheable`, cache bị evict khi admin update setting).

Để trống `ai.suggestion.model` = quay về dùng `ai.model` chung.

### Cấu hình KHÔNG chỉnh nóng

| Giá trị | Ở đâu | Lý do |
|---|---|---|
| `rag.url`, `rag.read-timeout-ms` | `application.yml` | Hạ tầng, dùng chung cho cả chat |
| Timeout của cache gợi ý (20 phút) | hằng số trong `RedisCacheConfig` | Class này tạo `CacheManager`, mà `SystemSettingService` được cache qua chính nó → đọc setting ở đó sẽ tạo vòng lặp khởi tạo |

## Kiến trúc code

| File | Vai trò |
|---|---|
| `ai/controller/user/AiSuggestionUserController.java` | REST endpoint |
| `ai/service/AiSuggestionService.java` | Guard prefix, cache, semaphore, parse/filter output |
| `ai/service/RagService.java` → `generalQuestionsRaw(prompt)` | Dựng request + gọi RAG completion |
| `ai/service/api/RagApiService.java` → `general(dto, timeout)` | Endpoint `/generate/v1/chat/completions_simple` |
| `ai/api/RagApiCore.java` → `postForString(endPoint, body, timeout)` | Transport (overload timeout) |
| `ai/constant/AiPromptTemplates.java` → `questionSuggestionPrompt(...)` | Prompt |
| `ai/constant/AiSuggestionConfig.java` | Key + default của System Settings |
| `ai/util/AiTextSanitizer.java` | Làm sạch output (`<thinking>`, nhãn, câu dẫn, dấu nháy) |
| `ai/constant/CacheName.java` → `AI_SUGGESTION` | Tên cache Redis |
| `ai/configuration/RedisCacheConfig.java` | TTL riêng cho cache gợi ý |

### Điểm nhỏ đã sửa khi refactor

- `RagService.generateString(...)` được tách: phần trích `choices[0].message.content` chuyển
  thành helper `extractCompletionContent(response)` để `generalQuestionsRaw` tái dùng, tránh
  lặp logic parse.

## Cấu trúc prompt

`AiPromptTemplates.questionSuggestionPrompt(prefix, languageHint, count, maxItemChars)` theo
convention của repo (delimiter `<<<INPUT ... INPUT>>>`, mỗi luật một dòng, output contract
tường minh):

- Ngôn ngữ lấy từ System Setting `system.language` (mặc định `vi`).
- Yêu cầu trả về **duy nhất** JSON `{"suggestions": [...]}`.
- Mỗi gợi ý phải bắt đầu bằng chính xác prefix.
- Cấm markdown, đánh số, dấu nháy bao, giải thích, trình bày suy luận, bịa thông tin.

Phía service vẫn có **fallback tách theo dòng** nếu model không trả JSON (model nhỏ đôi khi
trả text), và bỏ code fence nếu có.

## Kết quả kiểm thử (2026-09-21, sau refactor)

| Case | Kết quả | Latency |
|---|---|---|
| Happy path `"giá xăng dầu"` | 200, 5 gợi ý đều bắt đầu bằng prefix | 4316 ms |
| Cache hit (cùng prefix) | 200, cùng kết quả | **66 ms** (~65×) |
| Prefix mới `"lãi suất"` | 200, 5 gợi ý | 1640 ms |
| Prefix quá ngắn `"gi"` | 200, `data: []` (không gọi model) | 42 ms |
| Prefix rỗng `""` | 400, code `1206` | 49 ms |
| **RAG down + cache miss** | **200, `data: []`**, log WARN `Connection refused` | 914 ms |

Log xác nhận đúng đường ống:

```
INFO  a.a.RagApiCore: RAG POST /generate/v1/chat/completions_simple request body:
WARN  a.s.AiSuggestionService: Không sinh được gợi ý cho prefix '...': Connection refused
```

Ghi chú latency: model `gpt-oss:20b` qua RAG service ~1.6–4.3s. Nếu thấy chậm, có thể set
`ai.suggestion.model` sang model nhỏ hơn (khi RAG hỗ trợ) mà không cần đổi model của chat.

## Frontend

1. **Debounce 250-350ms** trước khi gọi — biện pháp giảm tải quan trọng nhất.
2. **Không gọi khi prefix < 3 ký tự** (backend cũng chặn, nhưng tránh round-trip vô ích).
3. **Huỷ request cũ** khi có request mới (AbortController) để gợi ý của prefix cũ không ghi
   đè lên prefix mới.
4. Bấm gợi ý → **thay thế toàn bộ** text trong ô nhập bằng gợi ý đó.
5. `data` là mảng rỗng → **ẩn dropdown**, không hiển thị thông báo lỗi.

## Hạn chế đã biết

- **Chỉ dựa theo text đang gõ**, không dùng ngữ cảnh Topic/Notebook. Mở rộng bằng cách thêm
  tham số context (tên source, message gần nhất) vào prompt builder.
- **Không cache khi prefix < `minPrefixChars`** và không cache kết quả rỗng.
- **Không có rate limit theo user** — mới có semaphore toàn cục.
- **Không lưu lịch sử gợi ý** — không có bảng DB, không có migration (ngoài V36 seed settings).
- **Latency phụ thuộc model RAG đang dùng**. Nếu model hiện tại chậm, dùng
  `ai.suggestion.model` để trỏ sang model nhanh hơn.

## Đã gỡ bỏ khi refactor (2026-09-21)

| Thành phần | Ghi chú |
|---|---|
| `ai/api/OllamaApiCore.java` | Xoá — dùng `RagApiCore` chung |
| `ai/dto/outer/ollama/OllamaChatRequestDto.java` | Xoá cả package `ollama` |
| bean `ollamaWebClient` trong `ApiClientConfig` | Xoá — dùng `ragWebClient` chung |
| block `suggestion:` trong `application.yml` | Xoá — chuyển sang System Settings |
| class `AppProperties.Suggestion` | Xoá |
| 2 hằng `DEFAULT_SUGGESTION_*_TIMEOUT_MS` | Xoá |
| Tham số `AppProperties` trong `RedisCacheConfig.cacheManager` | Bỏ (không còn cần) |

Lý do: tránh **hai đường ống sinh text song song**, và để việc chọn model thuộc về RAG service
thay vì hard-code trong core-service.
