# Feedback System - Phase 1, 2 & 3 Complete

## Đã hoàn thành

### 1. Enum `FeedbackStatus`
**File**: `core/src/main/java/ai/enums/FeedbackStatus.java`

```java
public enum FeedbackStatus {
    PENDING("PENDING", "Chờ xử lý"),
    PROCESSING("PROCESSING", "Đang xử lý"),
    RESOLVED("RESOLVED", "Đã giải quyết"),
    REJECTED("REJECTED", "Đã từ chối");
}
```

### 2. Entity `FeedbackEntity`
**File**: `core/src/main/java/ai/entity/postgres/FeedbackEntity.java`

**Fields**:
- `id`: UUID (primary key)
- `subject`: String (tên góp ý)
- `content`: String (nội dung chi tiết)
- `isPrivate`: boolean (công khai/tư nhân)
- `status`: FeedbackStatus (trạng thái xử lý)
- `responseContent`: String (nội dung phản hồi)
- `responseDate`: Instant (thời gian phản hồi)
- `sender`: UserEntity (người gửi)
- `senderOrg`: OrganizationEntity (đơn vị người gửi)
- `responder`: UserEntity (người phản hồi - admin)
- `responderOrg`: OrganizationEntity (đơn vị phản hồi)
- `audit`: AuditEmbed (created/updated timestamps)

**Indexes**:
- `idx_feedbacks_sender_id`
- `idx_feedbacks_status`
- `idx_feedbacks_created_at`
- `idx_feedbacks_sender_org_id`

### 3. Flyway Migration
**File**: `core/src/main/resources/db/migration/V28__create_feedbacks.sql`

**Features**:
- CREATE TABLE với đầy đủ constraints
- Indexes cho performance
- Comments tiếng Việt cho các columns
- Default values (status=PENDING, isPrivate=false)

### 4. Repository `FeedbackRepository`
**File**: `core/src/main/java/ai/repository/FeedbackRepository.java`

**Methods**:
- `findBySenderId(UUID)` - tìm feedback của user
- `findBySenderOrgId(UUID)` - tìm feedback theo org
- `findByStatus(FeedbackStatus)` - tìm theo trạng thái
- `findAllByStatusIn(Collection<FeedbackStatus>)` - admin filter nhiều status
- `searchByKeyword(String)` - tìm kiếm theo subject/content
- `findAllBySenderOrgIds(Collection<UUID>)` - admin query theo nhiều orgs
- `countPendingFeedbacks()` - đếm feedback chờ xử lý

---

## Phase 2: DTO và Mapper

### DTOs đã tạo:
1. **FeedbackCreateRequestDto** - tạo góp ý mới
2. **FeedbackUpdateRequestDto** - cập nhật góp ý (user)
3. **FeedbackRespondRequestDto** - phản hồi góp ý (admin)
4. **FeedbackStatusUpdateRequestDto** - cập nhật trạng thái (admin)
5. **FeedbackFilterDto** - filter cho danh sách (extends PageableFilterDto)
6. **FeedbackResponseDto** - response DTO (trả về cho frontend)
7. **FeedbackMapper** - MapStruct mapper (extends GeneralMapper)

---

## Các bước tiếp theo - Phase 3: Service Layer

### Service methods đã implement:
```java
// FeedbackService
- FeedbackResponseDto createFeedback(FeedbackCreateRequestDto dto)
- Page<FeedbackResponseDto> getAllFeedbacks(FeedbackFilterDto filter)
- FeedbackResponseDto updateFeedback(UUID id, FeedbackUpdateRequestDto dto)
- void deleteFeedback(UUID id)
- FeedbackResponseDto respondFeedback(UUID id, FeedbackRespondRequestDto dto)
- FeedbackResponseDto updateStatus(UUID id, FeedbackStatusUpdateRequestDto dto)
```

### Security đã implement:
- `createFeedback`: authenticated (TẤT CẢ users)
- `getAllFeedbacks`: authenticated (ADMINsuper admin)
- `updateFeedback`: authenticated (OWNER)
- `deleteFeedback`: authenticated (OWNER)
- `respondFeedback`: authenticated (ADMIN)
- `updateStatus`: authenticated (ADMIN)

---

## Các bước tiếp theo - Phase 4: Controller Layer

### Controller endpoints cần implement:
- `POST /admin/feedbacks` - createFeedback (authenticated)
- `GET /admin/feedbacks` - getAllFeedbacks (authenticated, admin only)
- `GET /admin/feedbacks/{id}` - getFeedbackById (authenticated, owner or admin)
- `PATCH /admin/feedbacks/{id}` - updateFeedback (authenticated, owner only)
- `POST /admin/feedbacks/{id}/respond` - respondFeedback (authenticated, admin only)
- `PATCH /admin/feedbacks/{id}/status` - updateStatus (authenticated, admin only)
- `DELETE /admin/feedbacks/{id}` - deleteFeedback (authenticated, admin only)

---

**Status**: ✅ Phase 1, 2 & 3 hoàn tất
**Next**: 📋 Phase 4 - Controller Layer

**Status**: ✅ Phase 1 & 2 hoàn tất
**Next**: 📋 Phase 3 - Service Layer
