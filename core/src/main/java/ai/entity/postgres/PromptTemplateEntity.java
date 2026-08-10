package ai.entity.postgres;

import java.util.UUID;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import ai.entity.postgres.embeddable.AuditEmbed;
import ai.enums.PromptScope;
import ai.enums.PromptType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Prompt template — lưu các prompt/input nhanh thường dùng khi chat với Topic / NotebookLM.
 * <p>
 * Phân loại:
 * <ul>
 *   <li>{@code scope = SYSTEM} — admin tạo, dùng chung cho tất cả org (global), {@code owner = null}, {@code organization = null}.</li>
 *   <li>{@code scope = USER} — người dùng tự tạo, gắn với {@code owner} (user) và {@code organization} (org của user).</li>
 * </ul>
 * {@code promptType} xác định prompt dùng cho loại chatbot nào (TOPIC / NOTEBOOK / BOTH).
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "prompt_templates", indexes = {
        @Index(name = "idx_prompt_templates_scope", columnList = "scope"),
        @Index(name = "idx_prompt_templates_type", columnList = "prompt_type"),
        @Index(name = "idx_prompt_templates_owner", columnList = "owner_id"),
        @Index(name = "idx_prompt_templates_org", columnList = "organization_id")
})
@EntityListeners(AuditingEntityListener.class)
@Entity
public class PromptTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    UUID id;

    /**
     * Tên hiển thị của prompt (vd "Tóm tắt nội dung").
     */
    @Column(name = "title", nullable = false, length = 256)
    String title;

    /**
     * Nội dung prompt — chính là câu input nhanh sẽ được điền vào ô chat.
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    String content;

    /**
     * Loại chatbot mà prompt phục vụ: TOPIC / NOTEBOOK / BOTH.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "prompt_type", nullable = false, length = 32)
    PromptType promptType;

    /**
     * Phạm vi: SYSTEM (global) / USER (cá nhân).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 32)
    PromptScope scope;

    /**
     * Thứ tự hiển thị (nhỏ trước, lớn sau).
     */
    @Builder.Default
    @Column(name = "display_order", nullable = false)
    Integer displayOrder = 0;

    /**
     * Trạng thái hiệu lực. SYSTEM prompt bị tắt sẽ không hiển thị cho user.
     * USER prompt bị admin tắt sẽ ẩn khỏi danh sách của user.
     */
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    Boolean isActive = true;

    /**
     * Chủ sở hữu prompt (chỉ dùng cho scope=USER). SYSTEM prompt có owner = null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    UserEntity owner;

    /**
     * Org của prompt user (chỉ dùng cho scope=USER). SYSTEM prompt global nên organization = null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    OrganizationEntity organization;

    @Builder.Default
    @Embedded
    AuditEmbed audit = new AuditEmbed();
}
